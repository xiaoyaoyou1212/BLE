## FAQ

### 扫描不到设备？
首先确认该设备是不是低功耗设备，很多人会拿这个去扫描手机，这是扫描不到的，因为目前手机虽然是双模的，但是不会一直向外广播自己，一般都是作为中心设备。如果需要扫描手机需要用传统蓝牙的扫描方式 startDiscovery()，更多的使用详情可以参考我的博文[Android BLE学习笔记](http://blog.csdn.net/xiaoyaoyou1212/article/details/51854454)。

### 连接不上设备？
首先确认设备本身是否有问题，可以用其他蓝牙工具试下是否可以连接。如果确认设备没问题，那么用其他手机试试是否可以，多试试不同系统的，如果其他手机可以，那么可以确认是兼容性问题，因为不同的手机与设备组合后可能模块不能兼容，这个目前该框架没有做相关的兼容处理，如果你有好的处理方式，欢迎加群交流！

### 设备连接很慢？
这个是正常的，除非做了相关的模块兼容处理，一般第一次连接设备都会有点慢，只要不释放资源，后面连接都很快，还有部分 Android 7.0 的手机比其他的 Android 系统连接设备要慢。

### 连接设备时报 133 错误？
这个一般是没有调用 close 导致的问题，该框架已经做了相关处理，如果还出现该问题，那么可能是兼容性问题，建议换个手机再试试。

### 连接设备时报 257 错误？
这个是因为同一个设备在同一时刻有多次连接操作，导致蓝牙阻塞，建议使用同步机制控制连接操作。

### 发送数据时报 13 错误？
这个一般是该 UUID 对应的特征值不具备写入能力或者该通道发送的数据设备不能接收，建议换个有可写能力 UUID 生成的特征值试试。

### 连接成功后发送不了数据？
确保你选择的服务和特征值是支持可写的，可以用群里提供的demo APK试试写入数据。

### 连接成功后接收不到设备发送过来的数据？
确保你选择的服务和特征值是支持通知的，还有需要确定该UUID是可通知方式还是指示器方式，这两种方式是有区别的，指示器方式底层封装了应答机制，比可通知方式更可靠，在使用框架时确保传入的那个 isIndication 是否是对的。测试时可以先用群里提供的 demo APK 先验证是否可以收到设备的数据。

### 接收数据的可通知方式与指示器方式有什么区别，该怎么选择？
可通知方式：设备将要发送的数据直接发送，不管接收方有没有收到；
指示器方式：设备每发送一次数据都会等待接收方的应答，如果没有应答会重复发送，如果有应答才会进行下一次数据的发送。
这两种方式App端只需要根据需求选择就行，不需要关系是否需要应答，这个应答机制协议层已经做了封装。至于什么时候选择什么方式，一般情况是：如果是需要保证数据到达的准确性那么就选择指示器方式，而如果是只需要保证数据快速发送不太关心数据是否准确到达那么就选择可通知方式。

### 收发数据超过 20 字节怎么处理？

如果收发数据超过 20 字节，在发送时需要进行分包处理，接收时则需要进行组包处理。由于该库是基础的通信库，与数据处理等不进行挂钩，而组包一般与协议相关，故没有在该库中进行处理，而需要上层在调用数据发送和接收数据时统一进行处理。由于最近有人在使用库时问到分包的问题，故在此统一进行说明下，使用时可参考如下方式进行分包组包处理。

分包处理如下：
```
//外部调用发送数据方法
public void write(final BluetoothLeDevice bluetoothLeDevice, byte[] data) {
    if (dataInfoQueue != null) {
        dataInfoQueue.clear();
        dataInfoQueue = splitPacketFor20Byte(data);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                send(bluetoothLeDevice);
            }
        });
    }
}

//发送队列，提供一种简单的处理方式，实际项目场景需要根据需求优化
private Queue<byte[]> dataInfoQueue = new LinkedList<>();
private void send(final BluetoothLeDevice bluetoothLeDevice) {
    if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
        DeviceMirror deviceMirror = mDeviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        if (dataInfoQueue.peek() != null && deviceMirror != null) {
            deviceMirror.writeData(dataInfoQueue.poll());
        }
        if (dataInfoQueue.peek() != null) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    send(bluetoothLeDevice);
                }
            }, 100);
        }
    }
}

/**
 * 数据分包
 *
 * @param data
 * @return
 */
private Queue<byte[]> splitPacketFor20Byte(byte[] data) {
    Queue<byte[]> dataInfoQueue = new LinkedList<>();
    if (data != null) {
        int index = 0;
        do {
            byte[] surplusData = new byte[data.length - index];
            byte[] currentData;
            System.arraycopy(data, index, surplusData, 0, data.length - index);
            if (surplusData.length <= 20) {
                currentData = new byte[surplusData.length];
                System.arraycopy(surplusData, 0, currentData, 0, surplusData.length);
                index += surplusData.length;
            } else {
                currentData = new byte[20];
                System.arraycopy(data, index, currentData, 0, 20);
                index += 20;
            }
            dataInfoQueue.offer(currentData);
        } while (index < data.length);
    }
    return dataInfoQueue;
}
```

组包处理如下所示：
```
private byte[] buffer = new byte[1024];
private int bufferIndex = 0;

//数据组包处理，收到数据后就调用此方法
public void parse(byte[] bytes) {
    if (bytes == null) {
        return;
    }
    BleLog.i("receive packet:" + HexUtil.encodeHexStr(bytes));
    if (0 != bufferIndex) {//如果当前buffer有数据，就直接拷贝
        System.arraycopy(bytes, 0, buffer, bufferIndex, bytes.length);
    } else {//如果没有数据，判断当前的数据头部是不是协议头，这里默认协议头是0xFF
        if (bytes[0] == 0xFF && bufferIndex == 0) {
            //计算数据长度，根据协议中长度字段以及协议头、校验码长度
            bufferLength = ConvertUtil.bytesToIntHigh(new byte[]{bytes[1], bytes[2]}, 0) + 3;
            buffer = new byte[bufferLength];
            System.arraycopy(bytes, 0, buffer, 0, bytes.length);
        }
    }
    //数据包拷进来后要移位
    bufferIndex += bytes.length;
    final byte[] data = new byte[bufferIndex];
    System.arraycopy(buffer, 0, data, 0, data.length);
    if (isRightPacket(data)) {//判断数据是否符合协议要求
        BleLog.i("receive data:" + HexUtil.encodeHexStr(data));
        bufferIndex = 0;//位置清零
        receiveData(data);
    }
}

//数据处理
private void receiveData(byte[] data) {
    //处理组包后的数据
}
```

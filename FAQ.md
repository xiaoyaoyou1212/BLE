## FAQ

### 扫描不到设备？
首先确认该设备是不是低功耗设备，很多人会拿这个去扫描手机，这是扫描不到的，因为目前手机虽然是双模的，但是不会一直向外广播自己，一般都是作为中心设备。如果需要扫描手机需要用传统蓝牙的扫描方式startDiscovery()，更多的使用详情可以参考我的博文[Android BLE学习笔记](http://blog.csdn.net/xiaoyaoyou1212/article/details/51854454)。

### 连接不上设备？
首先确认设备本身是否有问题，可以用其他蓝牙工具试下是否可以连接。如果确认设备没问题，那么用其他手机试试是否可以，多试试不同系统的，如果其他手机可以，那么可以确认是兼容性问题，因为不同的手机与设备组合后可能模块不能兼容，这个目前该框架没有做相关的兼容处理，如果你有好的处理方式，欢迎加群交流！

### 设备连接很慢？
这个是正常的，除非做了相关的模块兼容处理，一般第一次连接设备都会有点慢，只要不释放资源，后面连接都很快，还有部分Android 7.0的手机比其他的Android系统连接设备要慢。

### 连接设备时报133错误？
这个一般是没有调用close导致的问题，该框架已经做了相关处理，如果还出现该问题，那么可能是兼容性问题，建议换个手机再试试。

### 连接成功后发送不了数据？
确保你选择的服务和特征值是支持可写的，可以用群里提供的demo APK试试写入数据。

### 连接成功后接收不到设备发送过来的数据？
确保你选择的服务和特征值是支持通知的，还有需要确定该UUID是可通知方式还是指示器方式，这两种方式是有区别的，指示器方式底层封装了应答机制，比可通知方式更可靠，在使用框架时确保传入的那个isIndication是否是对的。测试时可以先用群里提供的demo APK先验证是否可以收到设备的数据。

### 收发数据超过20字节怎么处理？

如果收发数据超过20字节，在发送时需要进行分包处理，接收时则需要进行组包处理。由于该库是基础的通信库，与数据处理等不进行挂钩，而组包一般与协议相关，故没有在该库中进行处理，而需要上层在调用数据发送和接收数据时统一进行处理。由于最近有人在使用库时问到分包的问题，故在此统一进行说明下，使用时可参考如下方式进行分包组包处理。

分包处理如下：
```
//存储待发送的数据队列
private Queue<byte[]> dataInfoQueue = new LinkedList<>();

private Handler handler = new Handler(){
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
    }
};

private Runnable runnable = new Runnable() {
    @Override
    public void run() {
        send();
    }
};

//外部调用发送数据方法
public void send(byte[] data) {
    if (dataInfoQueue != null) {
        dataInfoQueue.clear();
        dataInfoQueue = splitPacketFor20Byte(data);
        handler.post(runnable);
    }
}

//实际发送数据过程
private void send() {
    if (dataInfoQueue != null && !dataInfoQueue.isEmpty()) {
        //检测到发送数据，直接发送
        if (dataInfoQueue.peek() != null) {
            ViseBluetooth.getInstance().writeCharacteristic(dataInfoQueue.poll(), new IBleCallback<BluetoothGattCharacteristic>() {

                @Override
                public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

                }

                @Override
                public void onFailure(BleException exception) {

                }
            });
        }
        //检测还有数据，延时后继续发送，一般延时100毫秒左右
        if (dataInfoQueue.peek() != null) {
            handler.postDelayed(runnable, 100);
        }
    }
}

//数据分包处理
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
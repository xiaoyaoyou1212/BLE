# BLE
Android BLE基础操作框架，基于回调，操作简单。其中包含扫描、连接、广播包解析、服务读写及通知等功能。

- 项目地址：[https://github.com/xiaoyaoyou1212/BLE](https://github.com/xiaoyaoyou1212/BLE)

- 项目依赖：`compile 'com.vise.xiaoyaoyou:baseble:1.0.6'`

### QQ交流群
![QQ群](http://img.blog.csdn.net/20170327191310083)

### 版本说明
- V1.0.6
    - 修改日志打印，使用ViseLog库作为日志打印基础库，日志管理更方便；
    - 修改数据操作回调相关，将IBleCallback进行拆分，此处使用泛型不太合理，故去掉泛型操作，防止出现类转换异常。

- V1.0.5
    - 修复Android 5.0新扫描方式可能出现的空指针异常。

- V1.0.3
    - 增加针对Android 5.0以上系统的新扫描方式。

- V1.0.2
    - 优化初始化方式，将Context定义为Application级别。

- V1.0.1
    - 优化通知回调功能。

- V1.0.0
    - 项目初始提交。

版本号说明：版本号第一位为大版本更新时使用，第二位为小功能更新时使用，第三位则是用来bug修复管理。

### 常见问题

- #### 收发数据超过20字节怎么处理？

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

## 设备扫描
### 使用简介
扫描包含三种方式，第一种方式是直接扫描所有设备，可以设置循环扫描，也可以设置超时时间，扫描到的设备可以添加到`BluetoothLeDeviceStore`中统一进行处理，使用方式如下：
```
ViseBluetooth.getInstance().setScanTimeout(-1).startScan(new PeriodScanCallback() {
    @Override
    public void scanTimeout() {

    }

    @Override
    public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {
		bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
    }
});
```
第二种方式是扫描指定Mac地址的设备，一般需设置超时时间，扫描到指定设备后就停止扫描，使用方式如下：
```
ViseBluetooth.getInstance().setScanTimeout(5000).startScan(new PeriodMacScanCallback() {
    @Override
    public void scanTimeout() {

    }

    @Override
    public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {

    }
});
```
第三种方式是扫描指定广播名的设备，同第二种方式类似，也需设置超时时间，扫描到指定设备后也会停止扫描，使用方式如下：
```
ViseBluetooth.getInstance().setScanTimeout(5000).startScan(new PeriodNameScanCallback() {
    @Override
    public void scanTimeout() {

    }

    @Override
    public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {

    }
});
```
其中扫描到的设备信息都统一放到`BluetoothLeDevice`中，其中包含了设备的所有信息，以下会详细讲解具体包含哪些信息。
### 示例图
![设备扫描](http://img.blog.csdn.net/20160828100329779)

## 设备连接
### 使用简介
连接与扫描一样也有三种方式，第一种方式是在扫描获取设备信息`BluetoothLeDevice`后才可使用，可设置连接超时时间，默认超时时间为10秒，使用方式如下：
```
ViseBluetooth.getInstance().connect(bluetoothLeDevice, false, new IConnectCallback() {
    @Override
    public void onConnectSuccess(BluetoothGatt gatt, int status) {

    }

    @Override
    public void onConnectFailure(BleException exception) {

    }
    
    @Override
    public void onDisconnect() {
    
    }
});
```
第二种方式是连接指定Mac地址的设备，该方式使用前不需要进行扫描，该方式直接将扫描和连接放到一起，在扫描到指定设备后自动进行连接，使用方式如下：
```
ViseBluetooth.getInstance().connectByMac(mac, false, new IConnectCallback() {
    @Override
    public void onConnectSuccess(BluetoothGatt gatt, int status) {

    }

    @Override
    public void onConnectFailure(BleException exception) {

    }
    
    @Override
    public void onDisconnect() {
    
    }
});
```
第三种方式是连接指定名称的设备，该方式与第二种方式类似，使用方式如下：
```
ViseBluetooth.getInstance().connectByName(name, false, new IConnectCallback() {
    @Override
    public void onConnectSuccess(BluetoothGatt gatt, int status) {

    }

    @Override
    public void onConnectFailure(BleException exception) {

    }
    
    @Override
    public void onDisconnect() {
    
    }
});
```
连接成功后就可以进行相关处理，回调已在底层做了线程切换处理，可以直接操作视图。如果知道该设备服务的UUID，可直接调用`ViseBluetooth.getInstance().withUUIDString(serviceUUID, characteristicUUID, descriptorUUID);`，那么在下面操作设备时就不需要传特征(`BluetoothGattCharacteristic`)和描述(`BluetoothGattDescriptor`)相关参数，如果在连接成功后一直没设置UUID，那么在操作时则需要传该参数，该内容在下文的设备操作中会详细讲解，此处就不一一讲解了。
### 示例图
![设备连接](http://img.blog.csdn.net/20160828100240247)

## 设备详情
### 使用简介
#### DEVICE INFO(设备信息)
- 获取设备名称(Device Name):`bluetoothLeDevice.getName()`；
- 获取设备地址(Device Address):`bluetoothLeDevice.getAddress()`；
- 获取设备类别(Device Class):`bluetoothLeDevice.getBluetoothDeviceClassName()`；
- 获取主要设备类别(Major Class):`bluetoothLeDevice.getBluetoothDeviceMajorClassName()`；
- 获取服务类别(Service Class):`bluetoothLeDevice.getBluetoothDeviceKnownSupportedServices()`；
- 获取配对状态(Bonding State):`bluetoothLeDevice.getBluetoothDeviceBondState()`；

#### RSSI INFO(信号信息)
- 获取第一次信号时间戳(First Timestamp):`bluetoothLeDevice.getFirstTimestamp()`；
- 获取第一次信号强度(First RSSI):`bluetoothLeDevice.getFirstRssi()`；
- 获取最后一次信号时间戳(Last Timestamp):`bluetoothLeDevice.getTimestamp()`；
- 获取最后一次信号强度(Last RSSI):`bluetoothLeDevice.getRssi()`；
- 获取平均信号强度(Running Average RSSI):`bluetoothLeDevice.getRunningAverageRssi()`；

#### SCAN RECORD INFO(广播信息)
根据扫描到的广播包`AdRecordStore`获取某个广播数据单元`AdRecord`的类型编号`record.getType()`，再根据编号获取广播数据单元的类型描述`record.getHumanReadableType()`以及该广播数据单元的长度及数据内容，最后通过`AdRecordUtil.getRecordDataAsString(record)`将数据内容转换成具体字符串。

### 示例图
![设备详情](http://img.blog.csdn.net/20160828100259718) ![设备详情](http://img.blog.csdn.net/20160828100315766)

## 设备操作
### 使用简介
在操作设备前首先要保证设备已连接成功，那么在设备连接成功获取到`BluetoothGatt`后直接对服务的特征值UUID进行相关处理，其中特征值UUID有可读、可写、可通知、指示器四种，获取过程如下所示：
```
final String unknownServiceString = getResources().getString(R.string.unknown_service);
final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
final List<Map<String, String>> gattServiceData = new ArrayList<>();
final List<List<Map<String, String>>> gattCharacteristicData = new ArrayList<>();
mGattCharacteristics = new ArrayList<>();

// Loops through available GATT Services.
for (final BluetoothGattService gattService : gattServices) {
    final Map<String, String> currentServiceData = new HashMap<>();
    uuid = gattService.getUuid().toString();
    currentServiceData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
    currentServiceData.put(LIST_UUID, uuid);
    gattServiceData.add(currentServiceData);

    final List<Map<String, String>> gattCharacteristicGroupData = new ArrayList<>();
    final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
    final List<BluetoothGattCharacteristic> charas = new ArrayList<>();

    // Loops through available Characteristics.
    for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
        charas.add(gattCharacteristic);
        final Map<String, String> currentCharaData = new HashMap<>();
        uuid = gattCharacteristic.getUuid().toString();
        currentCharaData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
        currentCharaData.put(LIST_UUID, uuid);
        gattCharacteristicGroupData.add(currentCharaData);
    }

    mGattCharacteristics.add(charas);
    gattCharacteristicData.add(gattCharacteristicGroupData);
}
```
在获取到`BluetoothGattCharacteristic`后可进行如下操作：
- 设置通知服务
```
ViseBluetooth.getInstance().enableCharacteristicNotification(characteristic, new IBleCallback<BluetoothGattCharacteristic>() {
    @Override
    public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

    }

    @Override
    public void onFailure(BleException exception) {

    }
}, false);
```
其中最后一个参数是设置该通知是否是指示器方式，指示器方式为有应答的通知方式，在传输时更为靠谱。如果在连接成功时已经知道该设备可通知的UUID并且已经设置成功，那么此处还可以如下设置：
```
ViseBluetooth.getInstance().enableCharacteristicNotification(new IBleCallback<BluetoothGattCharacteristic>() {
    @Override
    public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

    }

    @Override
    public void onFailure(BleException exception) {

    }
}, false);
```
- 读取信息
```
ViseBluetooth.getInstance().readCharacteristic(characteristic, new IBleCallback<BluetoothGattCharacteristic>() {
    @Override
    public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

    }

    @Override
    public void onFailure(BleException exception) {

    }
});
```
同上，如果已设置过可读的UUID，那么此处也可以通过如下方式读取信息：
```
ViseBluetooth.getInstance().readCharacteristic(new IBleCallback<BluetoothGattCharacteristic>() {
    @Override
    public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

    }

    @Override
    public void onFailure(BleException exception) {

    }
});
```
- 写入数据
```
ViseBluetooth.getInstance().writeCharacteristic(characteristic, new byte[]{0x00,0x01,0x02}, new IBleCallback<BluetoothGattCharacteristic>() {
    @Override
    public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

    }

    @Override
    public void onFailure(BleException exception) {

    }
});
```
同样，如果在连接成功时设置过可写UUID，那么此处也可以通过如下方式写入数据：
```
ViseBluetooth.getInstance().writeCharacteristic(new byte[]{0x00,0x01,0x02}, new IBleCallback<BluetoothGattCharacteristic>() {
    @Override
    public void onSuccess(BluetoothGattCharacteristic bluetoothGattCharacteristic, int type) {

    }

    @Override
    public void onFailure(BleException exception) {

    }
});
```
此处的数据`new byte[]{0x00,0x01,0x02}`为模拟数据，在使用时替换为真实数据即可，切记每次发送的数据必须在20个字节内，如果大于20字节可采用分包机制进行处理。

### 示例图
![设备服务](http://img.blog.csdn.net/20160828100343826)

## 总结
从以上的描述中可以知道，设备相关的所有操作都统一交给`ViseBluetooth`进行处理，并且该类是单例模式，全局只有一个，管理很方便。使用前必须要在Application中调用`ViseBluetooth.getInstance().init(this);`进行初始化，在连接设备成功时会自动获得一个`BluetoothGatt`，在断开连接时会将该`BluetoothGatt`关闭，上层不用关心连接数最大为6的限制问题，只需要在需要释放资源时调用`ViseBluetooth.getInstance().clear();`就行，简单易用，这也正是该项目的宗旨。

## 感谢
在此要感谢两位作者提供的开源库[https://github.com/litesuits/android-lite-bluetoothLE](https://github.com/litesuits/android-lite-bluetoothLE)和[https://github.com/alt236/Bluetooth-LE-Library---Android](https://github.com/alt236/Bluetooth-LE-Library---Android)，这两个开源库对于本项目的完成提供了很大的帮助。

### 关于作者
#### 作者：胡伟
#### 网站：[http://www.huwei.tech](http://www.huwei.tech)
#### 博客：[http://blog.csdn.net/xiaoyaoyou1212](http://blog.csdn.net/xiaoyaoyou1212)

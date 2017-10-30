# BLE

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/880ed281aff445f890766ccccbe81d7d)](https://www.codacy.com/app/xiaoyaoyou1212/BLE?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=xiaoyaoyou1212/BLE&amp;utm_campaign=Badge_Grade) [![License](https://img.shields.io/badge/License-Apache--2.0-green.svg)](https://github.com/xiaoyaoyou1212/BLE/blob/master/LICENSE) [![API](https://img.shields.io/badge/API-18%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=18)

**Android BLE基础操作框架，基于回调，操作简单。包含扫描、多连接、广播包解析、服务读写及通知等功能。**

- **项目地址：**[https://github.com/xiaoyaoyou1212/BLE](https://github.com/xiaoyaoyou1212/BLE)

- **项目依赖：**`compile 'com.vise.xiaoyaoyou:baseble:2.0.0'`

## 功能
- **支持多设备连接管理；**

- **支持广播包解析；**

- **支持自定义扫描过滤条件；**

- **支持根据设备名称正则表达式过滤扫描设备；**

- **支持根据设备信号最小值过滤扫描设备；**

- **支持根据设备名称或 MAC 地址列表过滤扫描设备；**

- **支持根据设备 UUID 过滤扫描设备；**

- **支持根据指定设备名称或 MAC 地址搜索指定设备；**

- **支持连接设备失败重试；**

- **支持操作设备数据失败重试；**

- **支持绑定数据收发通道，同一种能力可绑定多个通道；**

- **支持注册和取消通知监听；**

- **支持配置最大连接数，超过最大连接数时会依据 Lru 算法自动断开最近最久未使用设备；**

- **支持配置扫描、连接和操作数据超时时间；**

- **支持配置连接和操作数据重试次数以及重试间隔时间。**

## 简介
打造该库的目的是为了简化蓝牙设备接入的流程。该库是 BLE 操作的基础框架，只处理 BLE 设备通信逻辑，不包含具体的数据处理，如数据的分包与组包等。该库提供了多设备连接管理，可配置最大连接数量，并在超过最大连接数时会依据 Lru 算法自动断开最近最久未使用设备。该库还定制了常用的扫描设备过滤规则，也支持自定义过滤规则。该库所有操作都采用回调机制告知上层调用的结果，操作简单，接入方便。

## 版本说明
[![LatestVersion](https://img.shields.io/badge/LatestVersion-2.0.0-orange.svg)](https://github.com/xiaoyaoyou1212/BLE/blob/master/VERSION.md)

## 代码托管
[![JCenter](https://img.shields.io/badge/JCenter-2.0.0-orange.svg)](https://jcenter.bintray.com/com/vise/xiaoyaoyou/baseble/2.0.0/)

## 常见问题
[![FAQ](https://img.shields.io/badge/FAQ-%E5%B8%B8%E8%A7%81%E9%97%AE%E9%A2%98-red.svg)](https://github.com/xiaoyaoyou1212/BLE/blob/master/FAQ.md)

## 效果展示
![设备扫描](http://img.blog.csdn.net/20160828100329779)
![设备连接](http://img.blog.csdn.net/20160828100240247)
![设备详情](http://img.blog.csdn.net/20160828100259718)
![设备详情](http://img.blog.csdn.net/20160828100315766)
![设备服务](http://img.blog.csdn.net/20160828100343826)

## 使用介绍

### 权限配置
6.0 以下系统不需要配置权限，库中已经配置了如下权限：
```
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
```
而如果手机系统在 6.0 以上则需要配置如下权限：
```
<uses-permission-sdk-23 android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```
因为蓝牙在 6.0 以上手机使用了模糊定位功能，所以需要添加模糊定位权限。

### 引入 SDK
在工程 module 的 build.gradle 文件中的 dependencies 中添加如下依赖：
```
compile 'com.vise.xiaoyaoyou:baseble:2.0.0'
```
构建完后就可以直接使用该库的功能了。

### 蓝牙初始化
在使用该库的功能前还需要进行初始化，可以是在 Application 中也可以是在 MainActivity 中：
```
//蓝牙相关配置修改
ViseBle.config()
        .setScanTimeout(-1)//扫描超时时间，这里设置为永久扫描
        .setConnectTimeout(10 * 1000)//连接超时时间
        .setOperateTimeout(5 * 1000)//设置数据操作超时时间
        .setConnectRetryCount(3)//设置连接失败重试次数
        .setConnectRetryInterval(1000)//设置连接失败重试间隔时间
        .setOperateRetryCount(3)//设置数据操作失败重试次数
        .setOperateRetryInterval(1000)//设置数据操作失败重试间隔时间
        .setMaxConnectCount(3);//设置最大连接设备数量
//蓝牙信息初始化，全局唯一，必须在应用初始化时调用
ViseBle.getInstance().init(this);
```
需要注意的是，蓝牙配置必须在蓝牙初始化前进行修改，如果默认配置满足要求也可以不进行配置。

### 设备扫描

其中扫描到的设备列表由 `BluetoothLeDeviceStore` 管理，而单个设备信息都统一放到`BluetoothLeDevice`中，其中包含了设备的所有信息，以下会详细讲解具体包含哪些信息。

### 设备连接

### 设备详情
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

### 发送数据

### 接收数据

### 读取数据

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

此处的数据`new byte[]{0x00,0x01,0x02}`为模拟数据，在使用时替换为真实数据即可，切记每次发送的数据必须在20个字节内，如果大于20字节可采用分包机制进行处理。

## 总结


## 感谢
在此要感谢两位作者提供的开源库[https://github.com/litesuits/android-lite-bluetoothLE](https://github.com/litesuits/android-lite-bluetoothLE)和[https://github.com/alt236/Bluetooth-LE-Library---Android](https://github.com/alt236/Bluetooth-LE-Library---Android)，这两个开源库对于本项目的完成提供了很大的帮助。

## 关于我
[![Website](https://img.shields.io/badge/Website-huwei-blue.svg)](http://www.huwei.tech/)
[![GitHub](https://img.shields.io/badge/GitHub-xiaoyaoyou1212-blue.svg)](https://github.com/xiaoyaoyou1212)
[![CSDN](https://img.shields.io/badge/CSDN-xiaoyaoyou1212-blue.svg)](http://blog.csdn.net/xiaoyaoyou1212)

## 最后
如果觉得该项目有帮助，请点下Star，您的支持是我开源的动力。如果有好的想法和建议，也欢迎Fork项目参与进来。使用中如果有任何问题和建议都可以进群交流，QQ群二维码如下：

![QQ群](http://img.blog.csdn.net/20170327191310083)


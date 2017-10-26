package com.example.sc3c;

/**
 * Created by 刘建南 on 2017/10/22.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * 对于蓝牙使用步骤如下：
 * 一、初始化蓝牙设备
 * 二、发现蓝牙设备
 *    1、设置蓝牙处于可见状态
 *    2、扫描蓝牙设备，这里可已扫描指定UUID的设备，也可以全部扫描 
 * 三、进行蓝牙的配对连接
 * 四、建立数据通信线程，进行通信
 * */

public class BlueTooth {
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private AppCompatActivity mActivity;

    //手机蓝牙名称
    private String localBluetoothName;
    //手机蓝牙地址
    private String localBluetoothAddr;
    //小车蓝牙地址
    private String bluetoothAddress;
    private boolean isPaired;
    private final String TAG="SC3C-APP";
    private final String SERIAL_PORT_UUID ="00001101-0000-1000-8000-00805F9B34FB";
    //小车蓝牙名称，暂时还不知道。。。
    private final String BLUETOOH_NAME="ljniphone";
    private final String PAIR_PIN="1234";

    //已配对过的蓝牙设备
    private Set<BluetoothDevice> pairedDevices;
    private IntentFilter mIntentFilter;



    public BlueTooth(Context context){
        mContext=context;
        mActivity=(AppCompatActivity)mContext;
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        localBluetoothName=mBluetoothAdapter.getName();
        localBluetoothAddr=mBluetoothAdapter.getAddress();
        Log.d(TAG,"手机蓝牙名称："+localBluetoothName+"\n手机蓝牙地址："+localBluetoothAddr);
        //是否支持蓝牙
        if(mBluetoothAdapter == null ){
            Toast.makeText(mContext,"This machine doesn't support bluetooth!",Toast.LENGTH_SHORT);
            mActivity.finish();
        }
        //检测蓝牙是否打开
        if(!mBluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            mActivity.startActivityForResult(intent,1);
        }

        //使蓝牙可见
        enableBluetoothDiscover();
        //搜索并连接蓝牙
        findBluetoothDevices();
    }


    //设置蓝牙可见
    private void enableBluetoothDiscover(){
        if(mBluetoothAdapter.getScanMode()
                != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            //设置搜索时长为300秒
            Intent disIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            disIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
            mContext.startActivity(disIntent);
        }
    }

    //查找原先已经配对过的蓝牙(以前配过对的),并开始扫描
    private void findBluetoothDevices(){
        //获取原先已经配对过的蓝牙设备
        pairedDevices=mBluetoothAdapter.getBondedDevices();
        Log.d(TAG,"获取完已配对设备");
        //判断是否曾经有配对过的设备
        if(pairedDevices.size() <= 0){
            //Toast.makeText(mContext,"没有已配对的设备",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"没有已配对的设备");
            try{
                //开始扫描并连接，一般是扫描12秒
                mBluetoothAdapter.startDiscovery();
                Log.d(TAG,"开始扫描");
                //要想搜索设备，需要注册一个BroadReceiver
                mIntentFilter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
                /*mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);  //发现新设备
                mIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);  //绑定状态改变
                mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);  //开始扫描
                mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  //结束扫描
                mIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);  //连接状态改变
                mIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);  //蓝牙开关状态改变*/
                mContext.registerReceiver(mBroadcastReceiver,mIntentFilter);
            }catch(Exception e){
                Log.d(TAG,e.toString());
            }
        }else{
            for(BluetoothDevice bluetoothDevice:pairedDevices){
                //小车蓝牙曾经和小车配对过
                if(bluetoothDevice.getName().equals(BLUETOOH_NAME)){
                    isPaired=true;
                    bluetoothAddress=bluetoothDevice.getAddress();
                    Log.d(TAG,"小车蓝牙名称"+BLUETOOH_NAME+"小车蓝牙地址"+bluetoothAddress);
                    //开始连接
                    beginConnect(bluetoothDevice);
                    break;
                }
            }
            //小车蓝牙没有配对过，开始扫描、连接
            if(!isPaired){
                //先扫描
                mBluetoothAdapter.startDiscovery();
                Log.d(TAG,"开始扫描");
                //要想搜索设备，需要注册一个BroadReceiver
                mIntentFilter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
               /* mIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);  //发现新设备
                mIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);  //绑定状态改变
                mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);  //开始扫描
                mIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);  //结束扫描
                mIntentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);  //连接状态改变
                mIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);  //蓝牙开关状态改变*/
                mContext.registerReceiver(mBroadcastReceiver,mIntentFilter);
            }
        }

    }

    //开始蓝牙的自动配对和连接，直接用工具类ClsUtils
    private void beginConnect(BluetoothDevice device){
        IntentFilter mPairIntentFilter;
        mPairIntentFilter=new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        mPairIntentFilter.setPriority(Integer.MAX_VALUE);
        mContext.registerReceiver(mPairReceiver,mPairIntentFilter);

        //配对前要判断是否这个蓝牙已经配对过
        if(device.getBondState() != BluetoothDevice.BOND_BONDED){
            try{
                ClsUtils.setPin(device.getClass(), device, PAIR_PIN); // 手机和蓝牙采集器配对
                ClsUtils.createBond(device.getClass(), device);
                ClsUtils.cancelPairingUserInput(device.getClass(), device);

                Log.d(TAG,"配对成功");//。。。

            }catch (Exception e){
                Log.d(TAG,"配对异常: "+e.toString());
            }
        }
        ClientThread mClientThread;
        mClientThread=new ClientThread(device);
        mClientThread.start();
    }

    //注销广播监听器
    public void unRegister(){
        mContext.unregisterReceiver(mBroadcastReceiver);
        mContext.unregisterReceiver(mPairReceiver);
    }


/*    final BluetoothAdapter.LeScanCallback scanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

        }
    };*/

    //广播接受监听器，用于扫描
    final BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            //每扫描到一个设备，系统都会发送一个广播：BluetoothDevice.ACTION_FOUND
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                try{
                    BluetoothDevice newDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG,"扫描到："+newDevice.getName()+"\n");
                    //!!!!!!!这里一定要判断扫描到的蓝牙名称是否是null！！！！
                    if(newDevice != null){
                        //跳过已配对的设备
                        if(!newDevice.equals(BluetoothDevice.BOND_BONDED)){
                            String deviceName=newDevice.getName();
                            //扫描到了小车蓝牙
                            if(deviceName.equals(BLUETOOH_NAME)){
                                Log.d(TAG,"已扫描到小车蓝牙");
                                //停止扫描
                                mBluetoothAdapter.cancelDiscovery();
                                bluetoothAddress=newDevice.getAddress();
                                //开始连接
                                beginConnect(newDevice);
                                isPaired=true;
                            }
                        }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                            if(!isPaired){
                                Toast.makeText(mContext,"没有搜索到蓝牙设备"+BLUETOOH_NAME, Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(mContext,"扫描结束，已搜索到"+BLUETOOH_NAME,Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }catch (Exception e){
                    Log.d(TAG,e.toString());
                }
            }
        }
    };

    //用于配对的广播监听器
    final BroadcastReceiver mPairReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals("android.bluetooth.device.action.PAIRING_REQUEST")){
                BluetoothDevice mPairDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try{
                    ClsUtils.setPin(mPairDevice.getClass(), mPairDevice, PAIR_PIN); // 手机和蓝牙采集器配对
                    ClsUtils.createBond(mPairDevice.getClass(), mPairDevice);
                    ClsUtils.cancelPairingUserInput(mPairDevice.getClass(), mPairDevice);

                }catch (Exception e){
                    Log.d(TAG,"配对异常: "+e.toString());
                }
            }
        }
    };

    private class ClientThread extends Thread{
        private BluetoothDevice mDevice;

        public ClientThread(BluetoothDevice device){
            mDevice=device;
        }

        @Override
        public void run(){
            BluetoothSocket mSocket;
            try{
                mSocket=mDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERIAL_PORT_UUID));
                Log.d(TAG,"准备连接蓝牙");
                mSocket.connect();
                Log.d(TAG,"连接成功");
                Toast.makeText(mContext,"蓝牙连接成功",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                Log.d(TAG,"socket进程错误"+e.toString());
            }
        }
    }

}


class ClsUtils {

    public ClsUtils() {
        // TODO Auto-generated constructor stub
    }
    /**
     * 与设备配对 参考<a href="https://www.2cto.com/ym/" target="_blank" class="keylink">源码</a>：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    static public boolean createBond(Class<? extends BluetoothDevice> btClass, BluetoothDevice btDevice)
            throws Exception
    {

        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    static public boolean removeBond(Class<? extends BluetoothDevice> btClass, BluetoothDevice btDevice)
            throws Exception
    {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    static public boolean setPin(Class btClass, BluetoothDevice btDevice,
                                 String str) throws Exception
    {
        try
        {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin",
                    new Class[]
                            {byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice,
                    new Object[]
                            {str.getBytes()});
            Log.e("returnValue设置密码", "" + returnValue.booleanValue());
            return returnValue.booleanValue();
        }
        catch (SecurityException e)
        {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;

    }

    // 取消用户输入
    static public boolean cancelPairingUserInput(Class<?> btClass,
                                                 BluetoothDevice device)

            throws Exception
    {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        cancelBondProcess(btClass,device) ;
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        Log.i("取消对话框","cancelPairingUserInput"+returnValue.booleanValue());
        return returnValue.booleanValue();
    }

    // 取消配对
    static public boolean cancelBondProcess(Class<?> btClass,
                                            BluetoothDevice device)

            throws Exception
    {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }

    /**
     *
     * @param clsShow
     */
    static public void printAllInform(Class<?> clsShow)
    {
        try
        {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++)
            {
                Log.e("method name", hideMethod[i].getName() + ";and the i is:"
                        + i);
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++)
            {
                Log.e("Field name", allFields[i].getName());
            }
        }
        catch (SecurityException e)
        {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    static public boolean pair(String strAddr, String strPsw)
    {
        boolean result = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();

        bluetoothAdapter.cancelDiscovery();

        if (!bluetoothAdapter.isEnabled())
        {
            bluetoothAdapter.enable();
        }



        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(strAddr);

        if (device.getBondState() != BluetoothDevice.BOND_BONDED)
        {
            try
            {
                Log.d("mylog", "NOT BOND_BONDED");
                boolean flag1=ClsUtils.setPin(device.getClass(), device, strPsw); // 手机和蓝牙采集器配对
                boolean flag2=ClsUtils.createBond(device.getClass(), device);
//                remoteDevice = device; // 配对完毕就把这个设备对象传给全局的remoteDevice

                result = true;


            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block

                Log.d("mylog", "setPiN failed!");
                e.printStackTrace();
            } //

        }
        else
        {
            Log.d("mylog", "HAS BOND_BONDED");
            try
            {
                ClsUtils.removeBond(device.getClass(), device);
                //ClsUtils.createBond(device.getClass(), device);
                boolean flag1= ClsUtils.setPin(device.getClass(), device, strPsw); // 手机和蓝牙采集器配对
                boolean flag2=ClsUtils.createBond(device.getClass(), device);
//                remoteDevice = device; // 如果绑定成功，就直接把这个设备对象传给全局的remoteDevice

                result = true;


            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                Log.d("mylog", "setPiN failed!");
                e.printStackTrace();
            }
        }
        return result;
    }

}
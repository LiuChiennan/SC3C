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

import java.util.Set;
import java.util.UUID;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * 对于蓝牙使用步骤如下：
 * 一、初始化蓝牙设备
 * 二、发现蓝牙设备
 *    1、设置蓝牙处于可见状态
 *    2、扫描蓝牙设备，这里可已扫描指定UUID的设备，也可以全部扫描 
 * 三、进行蓝牙的连接
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
    //小车蓝牙名称
    private final String BLUETOOH_NAME="";

    //已配对过的蓝牙设备
    private Set<BluetoothDevice> pairedDevices;
    private IntentFilter mIntentFilter;



    public BlueTooth(Context context){
        mContext=context;
        mActivity=(AppCompatActivity)mContext;
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        localBluetoothName=mBluetoothAdapter.getName();
        localBluetoothAddr=mBluetoothAdapter.getAddress();
        Log.d(TAG,"手机蓝牙名称："+localBluetoothName+"手机蓝牙地址："+localBluetoothAddr);
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
        //判断是否曾经有配对过的设备
        if(pairedDevices.size() <= 0){
            //Toast.makeText(mContext,"没有已配对的设备",Toast.LENGTH_SHORT).show();
            Log.d(TAG,"没有已配对的设备");
            //开始扫描并连接
            mBluetoothAdapter.startDiscovery();
            //要想搜索设备，需要注册一个BroadReceiver
            mIntentFilter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(mBroadcastReceiver,mIntentFilter);
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
                //要想搜索设备，需要注册一个BroadReceiver
                mIntentFilter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
                mContext.registerReceiver(mBroadcastReceiver,mIntentFilter);
            }
        }

    }

    //开始连接蓝牙
    private void beginConnect(BluetoothDevice device){
        ClientThread mClientThread;
        mClientThread=new ClientThread(device);
        mClientThread.start();
    }


/*    final BluetoothAdapter.LeScanCallback scanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

        }
    };*/

    //广播接受监听器
    final BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            //每扫描到一个设备，系统都会发送一个广播：BluetoothDevice.ACTION_FOUND
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice newDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //跳过已配对的设备
                if(!newDevice.equals(BluetoothDevice.BOND_BONDED)){
                    String deviceName=newDevice.getName();
                    //扫描到了小车蓝牙
                    if(deviceName.equals(BLUETOOH_NAME)){
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
        }
    };

    private class ClientThread extends Thread{
        private BluetoothDevice mDevice;

        public ClientThread(BluetoothDevice device){
            mDevice=device;
        }

        @Override
        public void run(){
            BluetoothSocket mSocket=null;
            try{
                mSocket=mDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERIAL_PORT_UUID));
                Log.d(TAG,"准备连接蓝牙");
                mSocket.connect();
                Log.d(TAG,"连接成功");
            }catch(Exception e){
                Log.d(TAG,"socket进程错误"+e.toString());
            }
        }
    }

}

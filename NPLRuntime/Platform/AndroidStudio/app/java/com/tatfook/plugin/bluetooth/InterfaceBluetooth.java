/****************************************************************************

****************************************************************************/
package plugin.Bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;

//require android api >= 21(5.0)
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Keep;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;


import plugin.Bluetooth.BluetoothLeService;
import com.tatfook.paracraft.ParaEngineWebView;
import com.tatfook.paracraft.ParaEngineActivity;
import com.tatfook.paracraft.ParaEngineLuaJavaBridge;
import com.tatfook.paracraft.LuaFunction;
import com.tatfook.paracraft.ParaEnginePluginInterface;
import com.tatfook.paracraft.ParaEnginePluginWrapper.PluginWrapperListener;

@Keep
public class InterfaceBluetooth implements ParaEnginePluginInterface{
    public final static String  LogTag = "ParaEngine";

    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // java call lua enum
    public final static int CHECK_DEVICE = 1101;
    public final static int SET_BLUE_STATUS = 1102;
    public final static int ON_READ_CHARACTERISTIC_FINSH = 1103;
    public final static int ON_CHARACTERISTIC = 1104;
    public final static int ON_DESCRIPTOR = 1105;

    private BluetoothAdapter mBluetoothAdapter;

    private boolean mScanning = false;
    private Handler mHandler;
    private String mDeviceAddress;

    public static ParaEngineActivity mMainActivity;

    private static final int REQUEST_ENABLE_BT = 1;

    private static final long SCAN_PERIOD = 3000;

    private BluetoothLeService mBluetoothLeService;

    private boolean mConnected = false;

    private static InterfaceBluetooth mSingle = null;

    private static LuaFunction mLuaFunction = null;

    public InterfaceBluetooth()
    {

    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(LogTag, "AppActivity: onServiceConnectedonServiceConnectedonServiceConnectedonServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.i("appactivity", "Unable to initialize Bluetooth");
                mMainActivity.finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))
            {
                Log.i(LogTag, "!-------AppActivity: blue :connect");
                mConnected = true;
                callBaseBridge(SET_BLUE_STATUS, "1");

                for(int i = 0; i < s_checkUuids.size(); ++i)
                {
                    s_checkUuidsForWarp.add(s_checkUuids.get(i));
                }
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))
            {
                Log.i(LogTag, "!-------AppActivity: blue :disconnect");
                mConnected = false;
                callBaseBridge(SET_BLUE_STATUS, "0");

                s_checkUuidsForWarp.clear();

                searchBlueDevice();
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                enableTXNotification(mBluetoothLeService.getSupportedGattServices());
                Log.i(LogTag, "!-------AppActivity: blue start:find serverice");
            }
            else if(BluetoothLeService.ACTION_DATA_CHARACTERISTIC.equals(action))
            {
                String uuid = intent.getStringExtra(BluetoothLeService.ON_CHARACTERISTIC_UUID);
                String io = intent.getStringExtra(BluetoothLeService.ON_CHARACTERISTIC_IO);
                String status = intent.getStringExtra(BluetoothLeService.ON_CHARACTERISTIC_STATUS);
                String data = intent.getStringExtra(BluetoothLeService.ON_CHARACTERISTIC_DATA);
                JSONObject luajs_value = new JSONObject();
                try
                {
                    luajs_value.put("uuid", uuid);
                    luajs_value.put("io", io);
                    luajs_value.put("status", status);
                    luajs_value.put("data", data);
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
                Log.i(LogTag, "!-------ACTION_DATA_CHARACTERISTIC status:" + status);
                callBaseBridge(ON_CHARACTERISTIC, luajs_value.toString());
            }
            else if (BluetoothLeService.ACTION_DATA_DESCRIPTOR.equals(action))
            {
                ///////////////////////////////////
                warpCheckUUid();
                //////////////////////////////////
                String uuid = intent.getStringExtra(BluetoothLeService.ON_DESCRIPTOR_UUID);
                String io = intent.getStringExtra(BluetoothLeService.ON_DESCRIPTOR_IO);
                String status = intent.getStringExtra(BluetoothLeService.ON_DESCRIPTOR_STATUS);

                JSONObject luajs_value = new JSONObject();
                try
                {
                    luajs_value.put("uuid", uuid);
                    luajs_value.put("io", io);
                    luajs_value.put("status", status);
                }
                catch(JSONException e)
                {
                    e.printStackTrace();
                }
                callBaseBridge(ON_DESCRIPTOR, luajs_value.toString());
            }
        }
    };

	private PluginWrapperListener mOnInitCallback = null;

	@Override
	public boolean onCreate(Context cxt, Bundle savedInstanceState, PluginWrapperListener listener)
	{
		mMainActivity = (ParaEngineActivity) cxt;

    	mHandler = new Handler();
    	if (!mMainActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
    		return false;
    	}

        mSingle = this;


    	if (ContextCompat.checkSelfPermission(mMainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(mMainActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

			mOnInitCallback = listener;

			return true;
		} else {
			getMBluetoothAdapter();

			return false;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				getMBluetoothAdapter();

				mSingle = this;
			}

			if (mOnInitCallback != null) {
				mOnInitCallback.onInit();
			}
		}
	}

	@Override
	public void onStart(){}

	@Override
	public void onStop() {}

	@Override
	public void onAppBackground(){}

	@Override
	public void onAppForeground(){}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){}

	@Override
	public void setDebugMode(boolean debug){}

	@Override
	public void onSaveInstanceState(Bundle outState){}



	public void onInit(Map<String, Object> cpInfo, boolean bDebug)
	{
		 Log.i(LogTag, "onInit:");
	}

	String mLuaPath;
	public static void registerLuaCall(String luaPath)
	{
		mSingle.mLuaPath = luaPath;
	}

	private static BluetoothGattCharacteristic getCharacteristic(String ser_uuid, String cha_uuid)
	{
		BluetoothGattCharacteristic retcha = null;

		UUID ser_uuid_ = UUID.fromString(ser_uuid);
		BluetoothGattService gattService = mSingle.mBluetoothLeService.getBluetoothGatt().getService(ser_uuid_);
		UUID cha_uuid_ = UUID.fromString(cha_uuid);
		if(gattService != null)
			retcha = gattService.getCharacteristic(cha_uuid_);
		return retcha;
	}

	private static BluetoothGattDescriptor getDescriptor(String ser_uuid, String cha_uuid, String desc_uuid)
	{
		BluetoothGattCharacteristic retcha = getCharacteristic(ser_uuid, cha_uuid);

		if(retcha != null)
		{
			UUID desc_uuid_ = UUID.fromString(desc_uuid);
			return retcha.getDescriptor(desc_uuid_);
		}
		return null;
	}

	public static void connectDevice(String deviceAddr)
	{
        mSingle.mDeviceAddress = deviceAddr;
        if (mSingle.mBluetoothLeService != null) {
			final boolean result = mSingle.mBluetoothLeService.connect(mSingle.mDeviceAddress);			
			Log.d(LogTag, "AppActivity: link bluetooth Connect request result=" + result);
			if(result)
				mSingle._stopScanLeDevice();
		}
	}



	public static void writeToCharacteristic(String ser_uuid, String cha_uuid, String wdata_str)
	{
		//Log.i(LogTag, "writeToCharacteristic: writeToCharacteristic:" + ser_uuid);
		BluetoothGattCharacteristic wcharacteristic = getCharacteristic(ser_uuid, cha_uuid);
		if(wcharacteristic != null)
		{
			byte[] wdata = HexString2Bytes(wdata_str);
    		wcharacteristic.setValue(wdata); 
			wcharacteristic.setWriteType(2);  		
    		mSingle.mBluetoothLeService.writeCharacteristic(wcharacteristic);
		}
		else
			Log.e(LogTag, "writeToCharacteristic: wcharacteristic is null");
	}


	public static byte[] HexString2Bytes(String str) {
		if(str == null || str.trim().equals("")) {
			return new byte[0];
		}

		byte[] bytes = new byte[str.length() / 2];
		for(int i = 0; i < str.length() / 2; i++) {
			String subStr = str.substring(i * 2, i * 2 + 2);
			bytes[i] = (byte) Integer.parseInt(subStr, 16);
		}

		return bytes;
	}

	public static String characteristicGetStrValue(String ser_uuid, String cha_uuid)
	{
		BluetoothGattCharacteristic characteristic = getCharacteristic(ser_uuid, cha_uuid);
		if(characteristic != null)
		{
			final byte[] data = characteristic.getValue();
			String currDataStr = BluetoothLeService.characteristicData2JsStrValue(data);
			Log.i(LogTag, "characteristicGetStrValue currDataStr:" + currDataStr);

			return currDataStr;
		}
		return null;
	}

	public static int characteristicGetIntValue(String ser_uuid, String cha_uuid, String wdata_str)
	{
		BluetoothGattCharacteristic characteristic = getCharacteristic(ser_uuid, cha_uuid);
		if(characteristic != null)
		{
        	int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int value = characteristic.getIntValue(format, 1);
            return value;
		}
		return 0;
	}

	public static void readCharacteristic(String ser_uuid, String cha_uuid)
	{
		BluetoothGattCharacteristic rcharacteristic = getCharacteristic(ser_uuid, cha_uuid);

		if(rcharacteristic != null)
		{
			mSingle.mBluetoothLeService.readCharacteristic(rcharacteristic);
		}        
		else
			Log.e(LogTag, "readCharacteristic is null" + ser_uuid + "," + cha_uuid);
	}

	public static void setCharacteristicNotification(String ser_uuid, String cha_uuid, boolean isNotify)
	{
		BluetoothGattCharacteristic characteristic = getCharacteristic(ser_uuid, cha_uuid);
		if(characteristic != null)
		{
			Log.i(LogTag, "setCharacteristicNotification-----------:" + ser_uuid + "," + cha_uuid + "," + isNotify);
			mSingle.mBluetoothLeService.setCharacteristicNotification(characteristic, isNotify);
		}
	}

	public static void setDescriptorNotification(String ser_uuid, String cha_uuid, String desc_uuid)
	{
		BluetoothGattCharacteristic characteristic = getCharacteristic(ser_uuid, cha_uuid);
		if(characteristic != null)
		{
			mSingle.mBluetoothLeService.setCharacteristicDescriptor(characteristic, UUID.fromString(desc_uuid));
		}
	}

	public static boolean isBlueConnected()
	{
		return mSingle.mConnected;
	}

	static ArrayList<String> s_checkUuids = new ArrayList<String>();
	static ArrayList<String> s_checkUuidsForWarp = new ArrayList<String>();

	static String s_deviceName = null;

	public static void setDeviceName(String deviceName)
	{
		Log.i(LogTag, "setDeviceName setDeviceName-----------:" + deviceName);
		s_deviceName = deviceName;
	}

	public static void setupBluetoothDelegate()
	{
		Log.i(LogTag, "setupBluetoothDelegate setupBluetoothDelegate-----------:");
		mSingle.searchBlueDevice();
	}

	
	public static void setCharacteristicsUuid(String serUuid, String chaUuid)
	{
		s_checkUuids.add(serUuid);
		s_checkUuids.add(chaUuid);
	}

    public static String readAllBlueGatt()
	{
		List<BluetoothGattService> gattServices = mSingle.mBluetoothLeService.getBluetoothGatt().getServices();

        if (gattServices == null) 
			return null;

		HashMap<String, String> luaTableMap = new HashMap<String, String>();
        String uuid = null;
		JSONObject luajs_value = new JSONObject();

		try 
		{
			for (BluetoothGattService gattService : gattServices) 
			{
				uuid = gattService.getUuid().toString();
				Log.i(LogTag, "charas-gattService-uuid:" + uuid);

				JSONObject serviceChild_js = new JSONObject();

				List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
				{
					String uuid_cha = gattCharacteristic.getUuid().toString();
					Log.i(LogTag, "charas-gattCharacteristic-uuid:" + uuid_cha);

					luaTableMap.put(uuid_cha, uuid);

					List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();

					JSONObject characteristicChild_js = new JSONObject();
					for (BluetoothGattDescriptor gattDescriptor : gattDescriptors)
					{
						String uuid_desc = gattDescriptor.getUuid().toString();
						characteristicChild_js.put(uuid_desc, "");
					}
					serviceChild_js.put(uuid_cha, characteristicChild_js);
				}
				luajs_value.put(uuid, serviceChild_js);
				return luajs_value.toString();
			}
		}
		catch(JSONException e) 
		{
			e.printStackTrace();
		}
		return null;
    }

	private static void callBaseBridge(int pId, String extData)
	{
		final String mergeData = "msg = [[" + pId + "_" + extData + "]]";
		final LuaFunction luaFunction = mLuaFunction;
		if(mSingle.mLuaPath != null)
		{
			mMainActivity.runOnGLThread(new Runnable() {
                        @Override
                        public void run() {
							String filePath = mSingle.mLuaPath;
							//Log.e(LogTag, "nplActivatenplActivatenplActivate:" + mergeData);
							ParaEngineLuaJavaBridge.nplActivate(filePath, mergeData);
                        }
                    });
			
		}
	}

	private void warpCheckUUid()
	{
		if(s_checkUuidsForWarp.size() >= 2)
		{
			String ser_uuid = s_checkUuidsForWarp.get(0);
			String cha_uuid = s_checkUuidsForWarp.get(1);
			BluetoothGattCharacteristic characteristic = getCharacteristic(ser_uuid, cha_uuid);
			mSingle.mBluetoothLeService.setCharacteristicNotification(characteristic, true);
			List<BluetoothGattDescriptor> gattDescriptors = characteristic.getDescriptors();
			for (BluetoothGattDescriptor gattDescriptor : gattDescriptors)
			{
				Log.i(LogTag, "-----------------------------------set notify ser:" + ser_uuid +",cha:" + cha_uuid + ",desc:"+gattDescriptor.getUuid());
				mSingle.mBluetoothLeService.setCharacteristicDescriptor(characteristic, gattDescriptor.getUuid());
			}
			//mSingle.mBluetoothLeService.readCharacteristic(characteristic);
			s_checkUuidsForWarp.remove(0);
			s_checkUuidsForWarp.remove(0);
		}
	}

    private void enableTXNotification(List<BluetoothGattService> gattServices) 
	{
		warpCheckUUid();
    }

	private void _stopScanLeDevice()
	{
		if(mScanning)
		{
			mScanning = false;
			if(mBluetoothAdapter != null)
			{
				BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
				if(bluetoothLeScanner != null)
					bluetoothLeScanner.stopScan(mLeScanCallback);
				//Log.i(LogTag, "-------------stop scanning");
			}
		}		
	}

	private void _startScanLeDevice()
	{
		if(!mScanning && !mConnected)
		{
			mScanning = true;
			if(mBluetoothAdapter!=null)
			{
				BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
				if(bluetoothLeScanner != null)
					bluetoothLeScanner.startScan(mLeScanCallback);
				//Log.i(LogTag, "-------------start scanning");
			}
		}
	}
    
	private void scanLeDevice(final boolean enable) {
		getMBluetoothAdapter();
        if (enable) {
			_startScanLeDevice();
			mHandler.postDelayed(new Runnable() {
						@Override
						public void run(){ 
							if(mConnected == false)
							{
								_stopScanLeDevice();
								scanLeDevice(true);
							}
						}
			}, SCAN_PERIOD);
        } else {
            _stopScanLeDevice();
        }
    }
	
	public void searchBlueDevice()
	{
		if(mConnected)
			return;

		//bluetooth permissions
		if (!mBluetoothAdapter.isEnabled()) {
	        if (!mBluetoothAdapter.isEnabled()) {
	            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            //mMainActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	        }
	    }
	    scanLeDevice(true);
	}

	public void onPause() {
		scanLeDevice(false);
	}

	public void onResume() {
		if(!mConnected)
		{
			//searchBlueDevice();
		    Log.i(LogTag, "onResume");
		    mMainActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
			if (mBluetoothLeService != null) {
			    if(mDeviceAddress!=null)
			    {
			    	final boolean result = mBluetoothLeService.connect(mDeviceAddress);
					Log.d(LogTag, "Connect request result=" + result);
			    }
			}
		}
	}
	

	public void onDestroy() {
		Log.i(LogTag, "appactivity-onDestroy");

		if(mBluetoothLeService!=null)
		{
			mBluetoothLeService.disconnect();
		}
		scanLeDevice(false);
		mMainActivity.unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	private ScanCallback mLeScanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			if (!mScanning) {
				return;
			}

			if(mConnected){
				return;
			}

			BluetoothDevice device = result.getDevice();

			if(s_deviceName != null)
			{
				if(device.getName() == null || device.getName().equals(s_deviceName) == false)
					return;
			}

			int checkrssi = result.getRssi();

            //Log.e(LogTag, "AppActivity: now bluetooth device:" + device.getName()+"//"+device.getAddress() + "//" + checkrssi);

			JSONObject luajs_value = new JSONObject();
			try
			{
				luajs_value.put("name", device.getName());
				luajs_value.put("addr", device.getAddress());
				luajs_value.put("rssi", checkrssi);
			}
			catch(JSONException e) 
			{
				e.printStackTrace();
			}
			callBaseBridge(CHECK_DEVICE, luajs_value.toString());
		}
		@Override
		public void onBatchScanResults(List<ScanResult> results) {
			super.onBatchScanResults(results);
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);
		}
	};

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_CHARACTERISTIC);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_DESCRIPTOR);
        return intentFilter;
    }
    
    private void getMBluetoothAdapter()
    {
    	if(mBluetoothAdapter == null)
		{
			if (ContextCompat.checkSelfPermission(mMainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return;

            final BluetoothManager bluetoothManager =
		    		(BluetoothManager) mMainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
		    mBluetoothAdapter = bluetoothManager.getAdapter();
		    if(mBluetoothAdapter!=null)
		    {
		    	Intent gattServiceIntent = new Intent(mMainActivity, BluetoothLeService.class);
		    	mMainActivity.bindService(gattServiceIntent, mServiceConnection, mMainActivity.BIND_AUTO_CREATE);
		    }
		}
    }
    
	
}
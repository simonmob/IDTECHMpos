package ke.co.ekenya.idtechmpos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.idtechproducts.device.Common;
import com.idtechproducts.device.ErrorCode;
import com.idtechproducts.device.ErrorCodeInfo;
import com.idtechproducts.device.IDTEMVData;
import com.idtechproducts.device.IDTMSRData;
import com.idtechproducts.device.IDT_UniPayIII;
import com.idtechproducts.device.OnReceiverListener;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ResDataStruct;
import com.idtechproducts.device.StructConfigParameters;
import com.idtechproducts.device.audiojack.tools.FirmwareUpdateTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnReceiverListener {

    ScrollView scrollViewTest;
    Context context;
    private Button btnConnect,btnSwipeOrTap,btnIccard,btnClear;
    private EditText edtSelection;
    Dialog dlgMenu,dlgLanguageMenu,dlgCompleteEMV,dlgOnlineAuth;
    private int dialogId = 0;  //authenticate_dialog: 0 complete_emv_dialog: 1 language selection: 2 menu_display: 3
    private IDT_UniPayIII device;
    private FirmwareUpdateTool fwTool;
    private String info = "";
    private boolean startSwipe = false;
    private String detail = "";
    private Handler handler = new Handler();
    private ResDataStruct _resData;
    private AlertDialog alertSwipe;
    private TextView tvConnectionstatus;
    private TextView dataText;
    private View rootView;
    private LayoutInflater layoutInflater;
    private ViewGroup viewGroup;
    boolean isError = false;
    String password = "";

    public static LinearLayout llyt_Scroll_test;

    private Runnable doSwipeProgressBar = new Runnable() {

        public void run() {
            if (startSwipe)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Please swipe/tap a card");
                builder.setView(layoutInflater.inflate(R.layout.frame_swipe, viewGroup, false));
                builder.setCancelable(false);
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        int ret = device.msr_cancelMSRSwipe();
                        if (ret == ErrorCode.SUCCESS){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context,"Swipe cancelled",Toast.LENGTH_LONG).show();
                                }
                            });
                            //infoText.setText("Swipe cancelled");
                        }
                        else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context,"Failed to cancel swipe",Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    }
                });
                alertSwipe = builder.create();
                alertSwipe.show();
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        context = MainActivity.this;
        if (device != null) {
            device.unregisterListen();
            device.release();
            device= null;
        }
        initializeReader();


        llyt_Scroll_test = (LinearLayout) findViewById(R.id.llyt_scroll_Test);//  view_bi.findViewById(R.id.llyt_scroll_BI);
        scrollViewTest = (ScrollView) findViewById(R.id.sclvTest);// view_bi.findViewById(R.id.sclv_BI);
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);


//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        tvConnectionstatus=(TextView)findViewById(R.id.tvConnectionStatus);

        btnConnect = (Button)findViewById(R.id.btnConnectReader);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runAutoConfig();
            }
        });
        btnSwipeOrTap = (Button)findViewById(R.id.btnSwipeOrTapCard);
        btnSwipeOrTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSwipe = true;
                int ret = device.msr_startMSRSwipe();
                //int  ret = device.ctls_startTransaction(1.00,0.00,0,30,null);
                if (ret == ErrorCode.SUCCESS) {
                    info = "Please swipe a card";
                    detail = "";
                    handler.post(doSwipeProgressBar);
                    PutMessage(info);
                    //handler.post(doUpdateStatus);
                }
                else {
                    info = "cannot swipe card\n";
                    info += "Status: "+device.device_getResponseCodeString(ret)+"";
                    detail = "";
                    //handler.post(doUpdateStatus);
                    PutMessage(info);
                }

            }
        });
        btnIccard = (Button)findViewById(R.id.btniccard);
        btnIccard.setEnabled(false);
        btnIccard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                info = "Starting EMV Transactionnn";
                //handler.post(doUpdateStatus);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PutMessage(info);
                    }
                });

                IDT_UniPayIII.emv_setAutoAuthenticateTransaction(true);
                IDT_UniPayIII.emv_setAutoCompleteTransaction(false);
                byte tags[] = {(byte) 0xDF, (byte) 0xEF, 0x1F, 0x02, 0x01, 0x00};
                IDT_UniPayIII.emv_allowFallback(true);
//                if (IDT_UniPayIII.emv_getAutoAuthenticateTransaction()) {
//                    device.emv_startTransaction(1.00, 0.00, 0, 30, tags, false);
//                }
//                else{
                    device.emv_startTransaction(1.00, 0.00, 0, 30, null, false);
//            }
            }
        });
        btnClear = (Button)findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                info = "Canceling EMV Transactionnn";
//                //handler.post(doUpdateStatus);
//                PutMessage(info);
//                ResDataStruct resData = new ResDataStruct();
//                device.emv_cancelTransaction(resData);

                    //Tab_Demo.llyt_Scroll_Demo.removeAllViews();
                llyt_Scroll_test.removeAllViews();

            }
        });
        scrollViewTest = (ScrollView)findViewById(R.id.sclvTest);


    }

    @Override
    public void onBackPressed() {
        releaseSDK();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        if (device != null)
            device.unregisterListen();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void initializeReader()
    {
        if (device != null){
            releaseSDK();
        }
        device = new IDT_UniPayIII(this, this);
        openReaderSelectDialog();

        String filepath = getXMLFileFromRaw();
        if(!isFileExist(filepath)) {
            filepath = null;
        }
        device.config_setXMLFileNameWithPath(filepath);
        device.config_loadingConfigurationXMLFile(true);
        device.log_setVerboseLoggingEnable(true);
        //fwTool = new FirmwareUpdateTool(this, context);
        displaySdkInfo();

    }

    public void releaseSDK() {
        if (device != null) {
            device.unregisterListen();
            device.release();
            device  = null;
        }
    }

    private boolean isFileExist(String path) {
        if(path==null)
            return false;
        File file = new File(path);
        if (!file.exists()) {
            return false ;
        }
        return true;
    }

    private String getXMLFileFromRaw(){
        //the target filename in the application path
        String fileName = "idt_unimagcfg_default.xml";

        try{
            InputStream in = getResources().openRawResource(R.raw.idt_unimagcfg_default);
            int length = in.available();
            byte [] buffer = new byte[length];
            in.read(buffer);
            in.close();
            context.deleteFile(fileName);
            FileOutputStream fout = context.openFileOutput(fileName, MODE_PRIVATE);
            fout.write(buffer);
            fout.close();

            // to refer to the application path
            File fileDir = context.getFilesDir();
            fileName = fileDir.getParent() + java.io.File.separator + fileDir.getName();
            fileName = fileName+java.io.File.separator+"idt_unimagcfg_default.xml";

        } catch(Exception e){
            e.printStackTrace();
            fileName = null;
        }
        return   fileName;
    }

    public void displaySdkInfo() {

        String info = 	"Manufacturer: " + android.os.Build.MANUFACTURER + "\n" +
                "Model: " + android.os.Build.MODEL + "\n" +
                "OS Version: " + android.os.Build.VERSION.RELEASE + " \n" +
                "SDK Version: \n" + device.config_getSDKVersion() + "\n";

        Toast.makeText(getApplicationContext(),info,Toast.LENGTH_LONG).show();

        //detail = "";

        //handler.post(doUpdateStatus);
    }

    void openReaderSelectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select a device:");
        builder.setCancelable(false);
        builder.setItems(R.array.reader_type, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                switch(which) {

                    case 0:
                        if (device.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III))
                            Toast.makeText(context, "UniPay III is selected", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(context, "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        if (device.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III_USB))
                            Toast.makeText(context, "UniPay III (USB-HID) is selected", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(context, "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        if (device.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III_BT))
                        {
                            Toast.makeText(context, "UniPay III (Bluetooth) is selected", Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(context, "Failed. Please disconnect first.", Toast.LENGTH_SHORT).show();
                }

                //device.setIDT_Device(fwTool);
                if (device.device_getDeviceType() == ReaderInfo.DEVICE_TYPE.DEVICE_UNIPAY_III_BT)
                {
//                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//                        Toast.makeText(context, "Bluetooth LE is not supported\r\n", Toast.LENGTH_LONG).show();
//                        return;
//                    }
//                    final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//                    mBtAdapter = bluetoothManager.getAdapter();
//                    if (mBtAdapter == null)
//                    {
//                        Toast.makeText(getActivity(), "Bluetooth LE is not available\r\n", Toast.LENGTH_LONG).show();
//                        return;
//                    }
//                    btleDeviceRegistered = false;
//                    if (!mBtAdapter.isEnabled()) {
//                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//                    } else {
//                        scanLeDevice(true, BLE_ScanTimeout);
//                    }
                } else {
                    device.registerListen();
                }
            }
        });
        builder.create().show();
    }

    void runAutoConfig() {
        //config = null;
        String filepath = getXMLFileFromRaw();
        if(!isFileExist(filepath)) {
            filepath = null;
        }
        if (ErrorCode.SUCCESS == device.autoConfig_start(filepath))
            Toast.makeText(context, "AutoConfig started", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(context, "AutoConfig not started", Toast.LENGTH_SHORT).show();
    }

    //Put message on the scroll view. Displays the actions taking place.
    private void PutMessage(String string) {
        TextView lb = new TextView(context);
        lb.setTextSize(16);
        lb.setText(string);

        if (llyt_Scroll_test.getChildCount() > 0)
            llyt_Scroll_test.addView(lb);
        else
            llyt_Scroll_test.addView(lb);
        if (llyt_Scroll_test.getChildCount() > 500) {
            llyt_Scroll_test.removeViewAt(llyt_Scroll_test.getChildCount() - 1);
        }
        llyt_Scroll_test.invalidate();

        scrollViewTest.post(new Runnable() {
            @Override
            public void run() {
                scrollViewTest.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    @Override
    public void swipeMSRData(IDTMSRData idtmsrData) {
        startSwipe = false;
        if (alertSwipe != null) {
            alertSwipe.dismiss();
        }

        if (idtmsrData.result != ErrorCode.SUCCESS){
            info = "MSR card data didn't read correctly\n";
            info += ErrorCodeInfo.getErrorCodeDescription(idtmsrData.result);
            PutMessage(info);
            PutMessage(detail);
            Toast.makeText(context, "Please Try again", Toast.LENGTH_SHORT).show();
            return;
        }else{
            info = ">>> MSR Card tapped/Swiped Successfully";
        }


        try {
            Log.i("card Tap/swipe data: ",new String(idtmsrData.cardData,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //Get data to be tranmitted to back end for processing. Fo now get track 2 data then prompt for Pin.
        //1. Check if is a swipe or Tap.

        byte[] ff8105 = idtmsrData.unencryptedTags.get("FF8105");
            if (idtmsrData.track2 != null && idtmsrData.track1 != null) { // this is a successful swipe.
                String[] track2data = idtmsrData.track2.split("=");

                String track2dataPAN = track2data[0].replace(";", "");
                //Show the dialog
                LayoutInflater li = LayoutInflater.from(context);
                View dialogView = li.inflate(R.layout.custom_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                // set title
                alertDialogBuilder.setTitle("PIN Input For Card No: " +track2dataPAN);
                // set custom dialog icon
                //alertDialogBuilder.setIcon(R.drawable.ic_launcher);
                // set custom_dialog.xml to alertdialog builder
                alertDialogBuilder.setView(dialogView);
                final EditText userInput = (EditText) dialogView
                        .findViewById(R.id.et_input);
                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        // get user input and set it to etOutput
                                        // edit text
                                        //etOutput.setText(userInput.getText());
                                        Toast.makeText(MainActivity.this, "Request sent for Processing", Toast.LENGTH_LONG).show();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.dismiss();
                                    }
                                });
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
            }else if ((idtmsrData.track2 == null || idtmsrData.track1 == null) && ff8105==null) {
                detail = Common.parse_MSRData(device.device_getDeviceType(), idtmsrData);
                Toast.makeText(context, "Please Try again. Track Data missing!", Toast.LENGTH_SHORT).show();
                info = ">> Track Data missing! ";
                PutMessage(info);
                PutMessage(detail);
                return;
            }


        String str = "";

        if(idtmsrData.unencryptedTags != null) {

            if (ff8105 != null) {//Successful Tap has ff8105 value
                String PanData[] = Common.getAsciiFromByte(ff8105).split("\n");
                String panString = PanData[2];
                String track2Data[] = panString.substring(31, 82).split("/");
                String cardNumber = track2Data[0].replace("^","");

                //Show the dialog
                LayoutInflater li = LayoutInflater.from(context);
                View dialogView = li.inflate(R.layout.custom_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context);
                // set title
                alertDialogBuilder.setTitle("PIN Input For Card No: " +cardNumber);
                // set custom dialog icon
                //alertDialogBuilder.setIcon(R.drawable.ic_launcher);
                // set custom_dialog.xml to alertdialog builder
                alertDialogBuilder.setView(dialogView);
                final EditText userInput = (EditText) dialogView
                        .findViewById(R.id.et_input);
                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        // get user input and set it to etOutput
                                        // edit text
                                        //etOutput.setText(userInput.getText());
                                        Toast.makeText(MainActivity.this, "Request sent for Processing", Toast.LENGTH_LONG).show();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.dismiss();
                                    }
                                });
                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                // show it
                alertDialog.show();
            }
        }
        detail = Common.parse_MSRData(device.device_getDeviceType(), idtmsrData);
        Log.i("Card Data",detail);
        PutMessage(info);
        PutMessage(detail);

    }

    @Override
    public void lcdDisplay(int mode, String[] lines, int timeout) {
        if (mode == 0x01){ //Menu Display
            dlgMenu = new Dialog(context);
            dlgMenu.setTitle("Application Menu");
            dlgMenu.setCancelable(false);
            dlgMenu.setContentView(R.layout.emv_menu_display_dialog);
            TextView tv = new TextView(context);
            tv = (TextView) dlgMenu.findViewById(R.id.tvApplication);
            String strApplication = "";
            for (int x = 0; x < lines.length; x++){
                strApplication = (strApplication + lines[x] + "\r\n");
            }
            tv.setText(strApplication);

           edtSelection = (EditText) dlgMenu.findViewById(R.id.edtAppSelection);

            Button btnMenuDisplayOK = (Button) dlgMenu.findViewById(R.id.btnMenuDisplayOK);
            btnMenuDisplayOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    byte bSelection = Byte.parseByte(edtSelection.getText().toString());
                    if (bSelection == 0x00)
                    {
                        Toast.makeText(context,"Selection Error: Cannot be 0",Toast.LENGTH_LONG).show();
                        return;
                    }
                    device.emv_lcdControlResponse((byte)0x01, (byte)bSelection);
                    dlgMenu.dismiss();
                }
            });
            dlgMenu.show();

            //wait for timeout
//            dialogId = 3;
//            timerDelayRemoveDialog((long)finalTimout * 1000, dlgMenu);

        } else if (mode == 0x08){ //Language Menu Display
            //Open a language menu
            dlgLanguageMenu = new Dialog(context);
            dlgLanguageMenu.setContentView(R.layout.language_menu);

            final ListView lv = (ListView ) dlgLanguageMenu.findViewById(R.id.lvLanguage);

            final int[] lineNum = new int[lines.length];

            for (int i=0;i<lines.length;i++)
            {
                lineNum[i] = Integer.valueOf(lines[i].substring(0, 1))  ;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.single_row_textview,
                    R.id.tvLanguage, lines);

            lv.setAdapter(adapter);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    device.emv_lcdControlResponse((byte)0x08, (byte)(lineNum[position] & 0xFF));
                    dlgLanguageMenu.dismiss();
                }
            });

            dlgLanguageMenu.setCancelable(false);
            dlgLanguageMenu.setTitle("Select Language");
            dlgLanguageMenu.show();

            //wait for timeout
//            dialogId = 2;
//            timerDelayRemoveDialog((long)finalTimout * 1000, dlgLanguageMenu);
        } else if (mode == 0x02) //Normal Display Get Function Key
        {
            Toast.makeText(context,"Weka Pin",Toast.LENGTH_LONG).show();
        } else{
            ResDataStruct toData = new ResDataStruct();
            info = lines[0];
            //handler.post(doUpdateStatus);
            PutMessage(info);
        }
    }

    @Override
    public void emvTransactionData(final IDTEMVData idtemvData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,"Kubaf....",Toast.LENGTH_LONG).show();
            }
        });

        detail = Common.emvErrorCodes(idtemvData.result);
        if(idtemvData.result == IDTEMVData.START_TRANS_SUCCESS) {
            detail += "Start transaction response:\r\n";
        }else if (idtemvData.result == IDTEMVData.GO_ONLINE){
            detail += "\"\\r\\n\">>>>Authentication response:\"\\r\\n\"";
        }else if (idtemvData.result == IDTEMVData.MSR_SUCCESS)
        {
            swipeMSRData(idtemvData.msr_cardData);
            detail += "\r\n" + this.emvErrorCodes(idtemvData.result) + "\r\n";
            return;
        }
        else {
            detail += "\r\n"+">>>> Complete Transaction response:"+"\r\n";
        }
        if (idtemvData.unencryptedTags != null && !idtemvData.unencryptedTags.isEmpty()){
            detail += "Unencrypted Tags:";
            Set<String> keys = idtemvData.unencryptedTags.keySet();
            for(String key: keys){
                detail += key + ": ";
                byte[] data = idtemvData.unencryptedTags.get(key);
                detail += Common.getHexStringFromBytes(data);
            }
        }
        if (idtemvData.maskedTags != null && !idtemvData.maskedTags.isEmpty()){
            detail += "Masked Tags:";
            Set<String> keys = idtemvData.maskedTags.keySet();
            for(String key: keys){
                detail += key + ": ";
                byte[] data = idtemvData.maskedTags.get(key);
                detail += Common.getHexStringFromBytes(data);
            }
        }
        if (idtemvData.encryptedTags != null && !idtemvData.encryptedTags.isEmpty()) {
            detail += "Encrypted Tags:";
            Set<String> keys = idtemvData.encryptedTags.keySet();
            for (String key : keys) {
                detail += key + ": ";
                byte[] data = idtemvData.encryptedTags.get(key);
                detail += Common.getHexStringFromBytes(data);
            }
        }

        //handler.post(doUpdateStatus);
        Log.i("EMV Details: ",detail);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            PutMessage(detail);

            }
        });


        if (idtemvData.result == IDTEMVData.GO_ONLINE){
            if (IDT_UniPayIII.emv_getAutoCompleteTransaction())
            {
                ResDataStruct resData = new ResDataStruct();
                int ret = completeTransaction(resData);
                if (ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND)
                {
                    //swipeButton.setEnabled(false);
                    //commandBtn.setEnabled(false);
                    Toast.makeText(context,"RETURN_CODE_OK_NEXT_COMMAND",Toast.LENGTH_LONG).show();
                }
                else
                {
                    info = "EMV Transaction Failed\n";
                    info += "Status: "+device.device_getResponseCodeString(ret)+"";
                    //swipeButton.setEnabled(true);
                    //commandBtn.setEnabled(true);
                }
            }
            else
            {
                dlgCompleteEMV = new Dialog(context);
                dlgCompleteEMV.setTitle("Complete EMV transaction");
                dlgCompleteEMV.setCancelable(false);
                dlgCompleteEMV.setContentView(R.layout.complete_emv_one_option_dialog);
                Button btnCompleteEMV = (Button) dlgCompleteEMV.findViewById(R.id.btnCompleteEMV);
                btnCompleteEMV.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //next step
                        ResDataStruct resData = new ResDataStruct();
                        int ret = completeTransaction(resData);
                        if (ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND)
                        {
                            //swipeButton.setEnabled(false);
                            //commandBtn.setEnabled(false);
                        }
                        else
                        {
                            info = "EMV Transaction Failed\n";
                            info += "Status: "+device.device_getResponseCodeString(ret)+"";
                            //swipeButton.setEnabled(true);
                            //commandBtn.setEnabled(true);
                        }
                        dlgCompleteEMV.dismiss();
                    }
                });
                Button btnCompCancel = (Button) dlgCompleteEMV.findViewById(R.id.btnCompEMVOneCancel);
                btnCompCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ResDataStruct resData = new ResDataStruct();
                        device.emv_cancelTransaction(resData);
                        if (false){
                           // dlgOnlineAuth.dismiss();
                        }
                        else if (true)
                            dlgCompleteEMV.dismiss();

                        info = "EMV Transaction Cancelled";
                        //handler.post(doUpdateStatus);
                        PutMessage(info);
                        //swipeButton.setEnabled(true);
                        //commandBtn.setEnabled(true);
                    }
                });
                //dialogId = 1;
                dlgCompleteEMV.show();
            }

        }
        else if (idtemvData.result == IDTEMVData.START_TRANS_SUCCESS){
            if (!IDT_UniPayIII.emv_getAutoAuthenticateTransaction())
            {
                //show authentication dialog
                dlgOnlineAuth = new Dialog(context);
                dlgOnlineAuth.setTitle("Request to authenticate");
                dlgOnlineAuth.setCancelable(false);
                dlgOnlineAuth.setContentView(R.layout.authenticate_dialog);
                Button btnAuthencate = (Button) dlgOnlineAuth.findViewById(R.id.btnAuthenticate);
                btnAuthencate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //next step
                        int ret = startAuthentication(_resData);
                        dlgOnlineAuth.dismiss();
                        if (ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND)
                        {
                            //swipeButton.setEnabled(false);
                            //commandBtn.setEnabled(false);
                        }
                        else
                        {
                            info = "EMV Transaction Failed\n";
                            info += "Status: "+device.device_getResponseCodeString(ret)+"";
                            //swipeButton.setEnabled(true);
                            //commandBtn.setEnabled(true);
                        }
                    }
                });
                Button btnAuthCancel = (Button) dlgOnlineAuth.findViewById(R.id.btnAuthCancel);
                btnAuthCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ResDataStruct resData = new ResDataStruct();
                        device.emv_cancelTransaction(resData);
                        if (dialogId == 0)
                            dlgOnlineAuth.dismiss();
                        else if (dialogId == 1)
                            dlgCompleteEMV.dismiss();

                        info = "EMV Transaction Cancelled";
                        //handler.post(doUpdateStatus);
                        PutMessage(info);
                        //swipeButton.setEnabled(true);
                        //commandBtn.setEnabled(true);
                    }
                });
                dialogId = 0;
                dlgOnlineAuth.show();
            }
        }
        else {
            if (idtemvData.result == IDTEMVData.TIME_OUT)
                info = "EMV transaction failed: TIME OUT.";

            ResDataStruct resData = new ResDataStruct();
            //swipeButton.setEnabled(true);
            //commandBtn.setEnabled(true);
            //handler.post(doUpdateStatus);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PutMessage(info);
                }
            });

            device.emv_cancelTransaction(resData);
            //totalEMVTime = System.currentTimeMillis() - totalEMVTime;
            detail += "\r\nTotal time for EMV transaction: "+"not available for now"+" ms\r\n";
        }

//        if (idtemvData.result == IDTEMVData.GO_ONLINE){
//            //Auto Complete
//            byte[] response = new byte[]{0x30,0x30};
//            device.emv_completeTransaction(false, response, null, null,null);
//        }else if (idtemvData.result == IDTEMVData.START_TRANS_SUCCESS){
//            //Auto Authenticate
//            device.emv_authenticateTransaction(null);
//        }
    }

    public int startAuthentication(ResDataStruct resData)
    {
        byte[] tlvElement = null;
        return device.emv_authenticateTransaction(tlvElement);
    }

    public int completeTransaction(ResDataStruct resData)
    {
        byte[] authResponseCode = new byte[]{(byte)0x30, 0x30};
        byte[] issuerAuthData = new byte[]{(byte)0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte)0x88, 0x30, 0x30};
        byte[] tlvScripts = null;
        byte[] value = null;
        return device.emv_completeTransaction(false, authResponseCode, issuerAuthData, tlvScripts, value);
    }


    private String emvErrorCodes(int val){
        if (val ==IDTEMVData.APPROVED_OFFLINE) return "APPROVED_OFFLINE";
        if (val ==IDTEMVData.DECLINED_OFFLINE) return "DECLINED_OFFLINE";
        if (val ==IDTEMVData.APPROVED) return "APPROVED";
        if (val ==IDTEMVData.DECLINED) return "DECLINED";
        if (val ==IDTEMVData.GO_ONLINE) return "GO_ONLINE";
        if (val ==IDTEMVData.CALL_YOUR_BANK) return "CALL_YOUR_BANK";
        if (val ==IDTEMVData.NOT_ACCEPTED) return "NOT_ACCEPTED";
        if (val ==IDTEMVData.USE_MAGSTRIPE) return "USE_MAGSTRIPE";
        if (val ==IDTEMVData.TIME_OUT) return "TIME_OUT";
        if (val ==IDTEMVData.START_TRANS_SUCCESS) return "START_TRANS_SUCCESS";
        if (val ==IDTEMVData.MSR_SUCCESS) return "MSR_SUCCESS";
        if (val ==IDTEMVData.FILE_ARG_INVALID) return "FILE_ARG_INVALID";
        if (val ==IDTEMVData.FILE_OPEN_FAILED) return "FILE_OPEN_FAILED";
        if (val ==IDTEMVData.FILE_OPERATION_FAILED) return "FILE_OPERATION_FAILED";
        if (val ==IDTEMVData.MEMORY_NOT_ENOUGH) return "MEMORY_NOT_ENOUGH";
        if (val ==IDTEMVData.SMARTCARD_FAIL) return "SMARTCARD_FAIL";
        if (val ==IDTEMVData.SMARTCARD_INIT_FAILED) return "SMARTCARD_INIT_FAILED";
        if (val ==IDTEMVData.FALLBACK_SITUATION) return "FALLBACK_SITUATION";
        if (val ==IDTEMVData.SMARTCARD_ABSENT) return "SMARTCARD_ABSENT";
        if (val ==IDTEMVData.SMARTCARD_TIMEOUT) return "SMARTCARD_TIMEOUT";
        if (val ==IDTEMVData.MSR_CARD_ERROR) return "MSR_CARD_ERROR";
        if (val ==IDTEMVData.PARSING_TAGS_FAILED) return "PARSING_TAGS_FAILED";
        if (val ==IDTEMVData.CARD_DATA_ELEMENT_DUPLICATE) return "CARD_DATA_ELEMENT_DUPLICATE";
        if (val ==IDTEMVData.DATA_FORMAT_INCORRECT) return "DATA_FORMAT_INCORRECT";
        if (val ==IDTEMVData.APP_NO_TERM) return "APP_NO_TERM";
        if (val ==IDTEMVData.APP_NO_MATCHING) return "APP_NO_MATCHING";
        if (val ==IDTEMVData.AMANDATORY_OBJECT_MISSING) return "AMANDATORY_OBJECT_MISSING";
        if (val ==IDTEMVData.APP_SELECTION_RETRY) return "APP_SELECTION_RETRY";
        if (val ==IDTEMVData.AMOUNT_ERROR_GET) return "AMOUNT_ERROR_GET";
        if (val ==IDTEMVData.CARD_REJECTED) return "CARD_REJECTED";
        if (val ==IDTEMVData.AIP_NOT_RECEIVED) return "AIP_NOT_RECEIVED";
        if (val ==IDTEMVData.AFL_NOT_RECEIVEDE) return "AFL_NOT_RECEIVEDE";
        if (val ==IDTEMVData.AFL_LEN_OUT_OF_RANGE) return "AFL_LEN_OUT_OF_RANGE";
        if (val ==IDTEMVData.SFI_OUT_OF_RANGE) return "SFI_OUT_OF_RANGE";
        if (val ==IDTEMVData.AFL_INCORRECT) return "AFL_INCORRECT";
        if (val ==IDTEMVData.EXP_DATE_INCORRECT) return "EXP_DATE_INCORRECT";
        if (val ==IDTEMVData.EFF_DATE_INCORRECT) return "EFF_DATE_INCORRECT";
        if (val ==IDTEMVData.ISS_COD_TBL_OUT_OF_RANGE) return "ISS_COD_TBL_OUT_OF_RANGE";
        if (val ==IDTEMVData.CRYPTOGRAM_TYPE_INCORRECT) return "CRYPTOGRAM_TYPE_INCORRECT";
        if (val ==IDTEMVData.PSE_BY_CARD_NOT_SUPPORTED) return "PSE_BY_CARD_NOT_SUPPORTED";
        if (val ==IDTEMVData.USER_LANGUAGE_SELECTED) return "USER_LANGUAGE_SELECTED";
        if (val ==IDTEMVData.SERVICE_NOT_ALLOWED) return "SERVICE_NOT_ALLOWED";
        if (val ==IDTEMVData.NO_TAG_FOUND) return "NO_TAG_FOUND";
        if (val ==IDTEMVData.CARD_BLOCKED) return "CARD_BLOCKED";
        if (val ==IDTEMVData.LEN_INCORRECT) return "LEN_INCORRECT";
        if (val ==IDTEMVData.CARD_COM_ERROR) return "CARD_COM_ERROR";
        if (val ==IDTEMVData.TSC_NOT_INCREASED) return "TSC_NOT_INCREASED";
        if (val ==IDTEMVData.HASH_INCORRECT) return "HASH_INCORRECT";
        if (val ==IDTEMVData.ARC_NOT_PRESENCED) return "ARC_NOT_PRESENCED";
        if (val ==IDTEMVData.ARC_INVALID) return "ARC_INVALID";
        if (val ==IDTEMVData.COMM_NO_ONLINE) return "COMM_NO_ONLINE";
        if (val ==IDTEMVData.TRAN_TYPE_INCORRECT) return "TRAN_TYPE_INCORRECT";
        if (val ==IDTEMVData.APP_NO_SUPPORT) return "APP_NO_SUPPORT";
        if (val ==IDTEMVData.APP_NOT_SELECT) return "APP_NOT_SELECT";
        if (val ==IDTEMVData.LANG_NOT_SELECT) return "LANG_NOT_SELECT";
        if (val ==IDTEMVData.TERM_DATA_NOT_PRESENCED) return "TERM_DATA_NOT_PRESENCED";
        if (val ==IDTEMVData.CVM_TYPE_UNKNOWN) return "CVM_TYPE_UNKNOWN";
        if (val ==IDTEMVData.CVM_AIP_NOT_SUPPORTED) return "CVM_AIP_NOT_SUPPORTED";
        if (val ==IDTEMVData.CVM_TAG_8E_MISSING) return "CVM_TAG_8E_MISSING";
        if (val ==IDTEMVData.CVM_TAG_8E_FORMAT_ERROR) return "CVM_TAG_8E_FORMAT_ERROR";
        if (val ==IDTEMVData.CVM_CODE_IS_NOT_SUPPORTED) return "CVM_CODE_IS_NOT_SUPPORTED";
        if (val ==IDTEMVData.CVM_COND_CODE_IS_NOT_SUPPORTED) return "CVM_COND_CODE_IS_NOT_SUPPORTED";
        if (val ==IDTEMVData.CVM_NO_MORE) return "CVM_NO_MORE";
        if (val ==IDTEMVData.PIN_BYPASSED_BEFORE) return "PIN_BYPASSED_BEFORE";
        return "";
    }

    @Override
    public void deviceConnected() {
        //Log.i("CardTransactions","Connected");
        //Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
        if (!Common.getBootLoaderMode())
        {
            String device_name = device.device_getDeviceType().toString();
            info = device_name.replace("DEVICE_", "");
            info += " Reader is connected\r\n";
            if (info.startsWith("UNIPAY_III_BT")) {
                info += "Address: " + "btleDeviceAddress";
            }

            //detail = "";
            //handler.post(doUpdateStatus);
            //Toast.makeText(context, info, Toast.LENGTH_LONG).show();
            tvConnectionstatus.setText("CONNECTED");
            btnConnect.setEnabled(false);
            PutMessage(info);
        }
       // Toast.makeText(getApplicationContext(), "Reader Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void deviceDisconnected() {
        //Toast.makeText(context, "Reader Disconnected", Toast.LENGTH_LONG).show();
        info = ">>>>> Reader Disconnected.";
        tvConnectionstatus.setText("DISCONNECTED");
        btnConnect.setEnabled(true);
        PutMessage(info);
    }

    @Override
    public void timeout(int errorCode) {
        info = ErrorCodeInfo.getErrorCodeDescription(errorCode);
    }

    @Override
    public void autoConfigCompleted(StructConfigParameters structConfigParameters) {
        Toast.makeText(context, "AutoConfig found a working profile.", Toast.LENGTH_LONG).show();
        device.device_connectWithProfile(structConfigParameters);
        Log.i("CardTransactions","Autoconfig complete");
    }

    @Override
    public void autoConfigProgress(int progressValue) {
        info = "AutoConfig is running: "+progressValue +"%";
        PutMessage(info);
//        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
//        Log.i("CardTransactions","Autoconfig running");
    }
    @Override
    public void msgRKICompleted(String s) {

    }

    @Override
    public void ICCNotifyInfo(byte[] bytes, String s) {

    }

    @Override
    public void msgBatteryLow() {

    }

    @Override
    public void LoadXMLConfigFailureInfo(int i, String s) {

    }

    @Override
    public void msgToConnectDevice() {

    }

    @Override
    public void msgAudioVolumeAjustFailed() {

    }

    @Override
    public void dataInOutMonitor(byte[] bytes, boolean b) {

    }
}

package soa.baldiva;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;

class ConnectBT extends AsyncTask<Void, Void, Void> {
    private boolean ConnectSuccess = true;
    private ProgressDialog progress;
    private MainActivity mainActivity;

    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ConnectBT (MainActivity mainActivity){
        progress = new ProgressDialog(mainActivity);
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onPreExecute() {
        progress.setTitle("Conectando...");
        progress.setMessage("Por favor, esperar.");
        progress.show();
        //progress = ProgressDialog.show(MainActivity.this, "Conectando...", "Por favor, esperar.");  //show a progress dialog
    }

    @Override
    protected Void doInBackground(Void... devices) {
        try {
            if (mainActivity.btSocket == null || ConnectSuccess) {
                BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(mainActivity.address);
                mainActivity.btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                mainActivity.btSocket.connect();
            }
        }
        catch (IOException e)
        {
            ConnectSuccess = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (!ConnectSuccess)
        {
            mainActivity.msg("La conexión falló. Inténtelo nuevamente.");
            mainActivity.finish();
        }
        else
        {
            mainActivity.msg("Conectado.");
        }
        progress.dismiss();
    }
}
package soa.baldiva;

import android.os.AsyncTask;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

class ReadThread extends AsyncTask<Void, String, Void> {
    private final InputStream mmInStream;
    private MainActivity mainActivity;

    ReadThread(MainActivity mainActivity) {
        InputStream tmpIn = null;
        this.mainActivity = mainActivity;
        try {
            tmpIn = mainActivity.btSocket.getInputStream();
        } catch (IOException e) {
            mainActivity.msg(e.getMessage());
        }
        mmInStream = tmpIn;
    }

    @Override
    protected Void doInBackground(Void... inputs) {
        byte[] buffer = new byte[256];
        int bytes;

        // Loop infinito para recibir los mensajes desde el sistema embebido
        // sale cuando es cancelado desde la activida principal
        while (true) {
            try {
                bytes = mmInStream.read(buffer);
                String readMessage = new String(buffer, 0, bytes);
                publishProgress(readMessage);
                if (isCancelled())
                    break;
            } catch (IOException e) {
                break;
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(String... inputs) {

        try {

            String readMessage = inputs[0];

            if (mainActivity.esta_volcado){
                mainActivity.pesoActual = "0.0000";
                mainActivity.esta_volcado=false;
            }else{
                mainActivity.pesoActual = readMessage;
            }
            //Muestra peso Actual
            mainActivity.txtPesoActual.setText(String.format("%s gr.", mainActivity.pesoActual));

            if(mainActivity.pesoUnitario !=null && !mainActivity.pesoUnitario.isEmpty() && isDouble(mainActivity.pesoUnitario)){
                double resultDivision;
                if(mainActivity.pesoActual!=null && !mainActivity.pesoActual.isEmpty() && isDouble(mainActivity.pesoActual)) {

                    resultDivision = Double.parseDouble(mainActivity.pesoActual)/Double.parseDouble(mainActivity.pesoUnitario);

                    long cantidad = Math.round(resultDivision);

                    String cantidadString = Long.toString(cantidad);
                    mainActivity.txtCantUnidades.setText(cantidadString);

                    if (cantidad >= mainActivity.contadorMax){
                        mainActivity.esta_volcado=true;
                        mainActivity.sendSenialDetenerCinta();
                        mainActivity.sendSenialVolcarTara();
                        mainActivity.msg("Espero un segundo. Volver a cargar.");

                        //Thread.sleep(3500);
                        mainActivity.sendSenialEncenderCinta();
                    }
                }
            }
        }catch (Exception e) {
            mainActivity.msg(e.getMessage());
        }
    }

    private boolean isDouble(String cadena){
        try{
            Double.parseDouble(cadena);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onPostExecute(Void result) {
    }

    @Override
    protected void onCancelled() {
        Toast.makeText(mainActivity.getBaseContext(), "Tarea cancelada!",
                Toast.LENGTH_SHORT).show();
    }
}

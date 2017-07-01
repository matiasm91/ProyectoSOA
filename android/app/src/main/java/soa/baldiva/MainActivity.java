package soa.baldiva;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements SensorEventListener {
    Button btnAumentar;
    Button btnDisminuir;
    Button btnFijarPeso;
    Button btnDesconectar, btnVolcarTara;
    Switch btnOnOffCinta;
    SeekBar barVelocidad;

    TextView txtPesoActual;
    TextView txtCantUnidades;
    TextView txtPesoUnitario;
    TextView txtMaxContador;

    String address = null;
    BluetoothSocket btSocket = null;

    long contadorMax;
    String pesoActual;
    String pesoUnitario;

    boolean esta_Encendido;
    boolean esta_volcado;
    boolean esta_volcado_anterior;

    SensorManager senSensorManager;
    Sensor senAccelerometer,proxSensor, senGiroscopio, senOrientacion;

    // Variables usadas para el cálculo del Shake
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 3000;

    //Thread que recibe los datos del sistema embebido
    private ReadThread mReadThread;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();

        //Recibe la dirección del dispositivo bluetooth
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);

        //Vista main activity
        setContentView(R.layout.activity_main);

        //Para inicializar la instancia de SensorManager, invocamos getSystemService,
        // que usaremos para acceder a los sensores del sistema,pasando el nombre del servicio.
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Con el gestor de sensores obtenemos una referencia a los sensores invocando
        // a getDefaultSensor y pasando el tipo de sensor que nos interesa.
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proxSensor= senSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        senGiroscopio=senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senOrientacion=senSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        btnAumentar = (Button)findViewById(R.id.button15);
        btnDisminuir = (Button)findViewById(R.id.button14);
        btnFijarPeso = (Button)findViewById(R.id.button11);
        btnVolcarTara = (Button)findViewById(R.id.button12);
        btnDesconectar = (Button)findViewById(R.id.button4);
        btnOnOffCinta = (Switch)findViewById(R.id.switch1);
        barVelocidad = (SeekBar)findViewById(R.id.seekBar);

        txtPesoActual = (TextView)findViewById(R.id.textView17);
        txtCantUnidades = (TextView)findViewById(R.id.textView4);
        txtPesoUnitario = (TextView)findViewById(R.id.textView20);
        txtMaxContador = (TextView)findViewById(R.id.textView18);

        contadorMax = 0;
        last_x=0;
        last_y=0;
        last_z=0;

        esta_Encendido = false;
        esta_volcado = false;
        esta_volcado_anterior = false;

        btnFijarPeso.setEnabled(false);
        btnVolcarTara.setEnabled(false);
        barVelocidad.setEnabled(false);
        btnOnOffCinta.setEnabled(false);

        new ConnectBT(this).execute();

        //Manejo de eventos
        btnAumentar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                aumentarCantidad();
            }
        });

        btnDisminuir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                disminuirCantidad();
            }
        });

        btnFijarPeso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                fijarPesoUnitario();
            }
        });

        btnVolcarTara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (esta_Encendido) {
                    sendSenialDetenerCinta();
                    sendSenialVolcarTara();
                    sendSenialEncenderCinta();
                } else {
                    sendSenialVolcarTara();
                }
            }
        });

        btnOnOffCinta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnOnOffCinta.isChecked()){
                    barVelocidad.setEnabled(true);
                    barVelocidad.setProgress(5);
                    sendSenialEncenderCinta();
                }else {
                    barVelocidad.setEnabled(false);
                    barVelocidad.setProgress(0);
                    sendSenialDetenerCinta();
                }
            }
        });

        btnDesconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect(); //close connection
            }
        });

        barVelocidad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {

                    try {
                        int valor = 100+(155/5) * progress;

                        if(esta_Encendido) {
                            String velocidad = String.valueOf(valor) + ":";
                            btSocket.getOutputStream().write(velocidad.getBytes());
                        }
                    } catch (IOException e) {
                        msg("Error al cambiar velocidad!");
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void Disconnect() {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                sendSenialDetenerCinta();
                btSocket.close();
            }
            catch (IOException e)
            { msg("Error al deconectar!");}
        }
        mReadThread.cancel(true);
        finish(); //Vuelve a la pantalla principal
    }

    private void disminuirCantidad() {
        this.contadorMax--;
        if(this.contadorMax < 0){
            btnFijarPeso.setEnabled(false);
            this.contadorMax = 0;
        }

        txtMaxContador.setText(String.valueOf(contadorMax));
    }

    private void aumentarCantidad() {
        this.contadorMax++;
        btnFijarPeso.setEnabled(true);
        txtMaxContador.setText(String.valueOf(contadorMax));
    }

    private void fijarPesoUnitario(){
        if(contadorMax>0) {
            this.pesoUnitario = this.pesoActual;
            txtPesoUnitario.setText(String.format("%s gr.", this.pesoUnitario));

            sendSenialEncenderCinta();
            btnVolcarTara.setEnabled(true);
            barVelocidad.setEnabled(true);
        }
    }

    void sendSenialEncenderCinta(){
        if (!esta_Encendido && btSocket!=null && contadorMax>0) {
            try {
                btSocket.getOutputStream().write("E:".getBytes());
                esta_Encendido = true;
                barVelocidad.setProgress(5);
                btnOnOffCinta.setChecked(true);
            } catch (IOException e) {
                    msg("Error al encender la cinta");
                }
        }
    }

    void sendSenialVolcarTara(){
        if (btSocket!=null) {
            try {
                btSocket.getOutputStream().write("V:".getBytes());
            }
            catch (IOException e) {
                msg("Error al volcar");
            }
        }
    }

    void sendSenialDetenerCinta(){
        if(esta_Encendido && btSocket != null) {
            try {
                btSocket.getOutputStream().write("D:".getBytes());
                esta_Encendido = false;
                barVelocidad.setProgress(0);
                btnOnOffCinta.setChecked(false);
            } catch (IOException e) {
                msg("Error al detener la cinta");
            }
        }
    }

    public void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Registramos los sensores utilizados para que nos envíen los eventos
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senGiroscopio, SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senOrientacion, SensorManager.SENSOR_DELAY_NORMAL);

        mReadThread = new ReadThread(this);
        mReadThread.execute();
    }

    @Override
    protected void onDestroy() {
        sendSenialDetenerCinta();
        mReadThread.cancel(true);
        super.onDestroy();
        finish();
    }

    @Override //Manejo de eventos de sensores
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD && !esta_Encendido) {
                    sendSenialEncenderCinta();
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        } else if (mySensor.getType() == Sensor.TYPE_PROXIMITY) {
            float valor = Float.parseFloat(String.valueOf(sensorEvent.values[0]));
            if(valor < 10 && esta_Encendido){
                sendSenialDetenerCinta();
            }
        } else if (mySensor.getType() == Sensor.TYPE_ORIENTATION) {

            float pitch = Float.parseFloat(String.valueOf(sensorEvent.values[1]));
            float roll = Float.parseFloat(String.valueOf(sensorEvent.values[2]));

//            txtPitch.setText(String.valueOf(pitch));
//            txtRoll.setText(String.valueOf(roll));


            if(roll<=5 && roll>=-5 && pitch> 170 && pitch< 190){
                esta_volcado=true;
            }else{
                esta_volcado=false;
                esta_volcado_anterior=false;
            }

            if(esta_volcado!=esta_volcado_anterior) {
                sendSenialDetenerCinta();
                sendSenialVolcarTara();
                sendSenialEncenderCinta();
            }

            esta_volcado_anterior=esta_volcado;


        } /*else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {

            float valorX = sensorEvent.values[0];
            float valorY = sensorEvent.values[1];
            float valorZ = sensorEvent.values[2];

            if(false){ //condición para volcar
                esta_volcado=true;
            }else{
                esta_volcado=false;
                esta_volcado_anterior=false;
            }

            if(esta_volcado!=esta_volcado_anterior) {
                sendSenialDetenerCinta();
                sendSenialVolcarTara();
                sendSenialEncenderCinta();
            }

            esta_volcado_anterior=esta_volcado;
        }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

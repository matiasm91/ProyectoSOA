#include <SoftwareSerial.h>

#include "HX711.h"
#include <Servo.h>

/////////////////////Variables globales////////////////////////

/* Variables globales del modulo de bluetooth. */
SoftwareSerial BTSerial(10, 11); //RX | TX
#define LED 8 //LED para simular el prendido y apagado de la maquina.

String mensaje_new;            // Mensaje nuevo recibido por Bluetooth
String mensaje_old;            // Mensaje viejo recibido por Bluetooth
/** Fin de variables globales del modulo de bluetooth. **/

/* Variables gloabales del modulo de la balanza y para controlar el servo. */
#define DOUT A1
#define CLK  A0

HX711 balanza(DOUT, CLK);
int pinMotor = 3;        // Pin de Motor de cinta transportadora
int pinServo = 6;        // Pin de ServoMotor
Servo servoMotor;
/* Fin de variables gloabales del modulo de la balanza y para controlar el servo. */

double peso;             // Peso actual en gramos
double pesoSimulado=0.0;

void setup() {
  //pinMode(9, OUTPUT); //This pin will pull the HC-05 pin 34 (Key pin
  //digitalWrite(9, HIGH);
  
  Serial.begin(9600); //Defino un serial para imprimir por consola arduino
  //Serial.println("Ingrese el comando AT:");
  BTSerial.begin(38400);  //HC-05 default speed in AT command more


  servoMotor.attach(pinServo);
  servoMotor.write(0);

  // Escala 2652 unidades = 1 gramo
  balanza.set_scale(1875);
  Serial.println("---------------------------------------------------------");
  Serial.println("               P E S A N D O   T A R A ");
  Serial.println("---------------------------------------------------------");

  balanza.tare(20);  //El peso actual es considerado el peso de la Tara. Se promedian 20 mediciones

  pinMode(pinMotor, OUTPUT);
  
  // Iniciamos el servo para que empiece a trabajar con el pin 6
  
}

void loop() {
  // Envía peso actual todo el tiempo
  enviarPeso();

  //Recepcion de mensaje desde Android
  recibirAccionDeAndroid();
}

void enviarPeso()
{
  char valorPesoString[10];
  
  // Se toma el promedio de 20 mediciones
  peso = balanza.get_units(20);
  //peso = simuladorPeso();

  // Peso siempre positivo
  if (peso < 0.0) {
    peso = 0.0;
  }
  /*Serial.print("Peso: ");
  Serial.print(peso);
  Serial.print(" - ");
  Serial.print("ValorPesoString: ");*/
  dtostrf(peso, 10, 4, valorPesoString);
  Serial.print("Peso: ");
  Serial.println(valorPesoString);
  
  BTSerial.write(valorPesoString);
  
  delay(800);
}

void recibirAccionDeAndroid() 
{
  String mensaje;
  char command;
   
    while(BTSerial.available()>0){
      // Existen datos para leer
      command = (byte)BTSerial.read();
      if(command == ':'){
        realizarAccion(mensaje);
        mensaje = "";
      }else {
        mensaje+=command;
      } 
      delay(1);
    }
}

void realizarAccion(String mensaje){
    Serial.print(mensaje);
    if(mensaje == "E" ) {    
    // Recepción - Encender cinta transportadora
    Serial.println("E - Encender cinta");
    encenderCinta();
  }else if(mensaje == "D" ) {
    // Recepción - Detener cinta transportadora
    Serial.println("D - Detener cinta");
    detenerCinta();
  }else if(mensaje == "V" ) {
    // Recepción - Volcar tara
    Serial.println("V - Volcar tara");
    volcarTara(150, 3000);
  } else if(mensaje != "" ) {
    Serial.print("Velocidad: ");
    Serial.println(mensaje);
    regularVelocidadCinta(mensaje);
  }
  }

void regularVelocidadCinta(String mensaje){
  unsigned long number;
  char copy[50];
  
  mensaje.toCharArray(copy, 50);
  
  number = strtoul(copy, NULL, 10 );
  Serial.print("Numero: ");
  Serial.println(number);
  analogWrite(pinMotor, number);

 }

void encenderCinta() {
  analogWrite(pinMotor, 255);
}

void detenerCinta() {
  analogWrite(pinMotor, 0);
}

void volcarTara(int anguloApertura, int tiempo) {
  servoMotor.write(0);
  servoMotor.write(anguloApertura);
  delay(tiempo);
  servoMotor.write(0);
}


double simuladorPeso(){
  pesoSimulado+=(((double)rand()/(double)RAND_MAX))*0.001;
  return pesoSimulado;
}


/*
  Proyecto IoT: Sistema de Control de Acceso
  Estudiantes: Benjamín Seguel y Marcelo Ducommun
  Asignatura: Aplicaciones Móviles para IOT
*/

#include <Keypad.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <Servo.h>

// --- Configuración de Actuadores ---
LiquidCrystal_I2C lcd(0x27, 16, 2);
Servo cerradura;
const int pinServo = 11;

// --- Configuración del Teclado ---
const byte FILAS = 4;
const byte COLUMNAS = 4;
char teclas[FILAS][COLUMNAS] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};
byte pinesFilas[FILAS] = {9, 8, 7, 6}; 
byte pinesColumnas[COLUMNAS] = {5, 4, 3, 2}; 
Keypad teclado = Keypad(makeKeymap(teclas), pinesFilas, pinesColumnas, FILAS, COLUMNAS);

// --- Configuración de Sensor ---
const int pinPIR = 10;

// Variables del sistema
String claveSecreta = "1234";
String claveIngresada = "";
bool presenciaDetectada = false;

void setup() {
  // Inicialización de actuadores
  lcd.init();
  lcd.backlight();
  cerradura.attach(pinServo);
  cerradura.write(0); // Cerradura cerrada inicial
  
  // Inicialización de sensores
  pinMode(pinPIR, INPUT);
  
  // Comunicación Serial (Para futura app móvil)
  Serial.begin(9600);
  
  lcd.setCursor(0, 0);
  lcd.print("Modo Vigilancia");
  Serial.println("Sistema Iniciado.");
}

void loop() {
  int estadoPIR = digitalRead(pinPIR);

  // Lógica del Sensor PIR
  if (estadoPIR == HIGH && !presenciaDetectada) {
    presenciaDetectada = true;
    lcd.clear();
    lcd.print("Ingrese PIN:");
    Serial.println("ALERTA: Movimiento detectado. Esperando credencial.");
    claveIngresada = "";
  }

  // Lógica de validación
  if (presenciaDetectada) {
    char tecla = teclado.getKey();
    
    if (tecla) {
      if (tecla == '#') { // Tecla para confirmar el PIN
        if (claveIngresada == claveSecreta) {
          // Actuadores en caso de éxito
          lcd.clear();
          lcd.print("Acceso Concedido");
          Serial.println("ACCESO CONCEDIDO: Abriendo puerta.");
          cerradura.write(90); // Abre la puerta
          delay(4000); 
          cerradura.write(0);  // Cierra la puerta
          
          // Reinicio al modo vigilancia
          lcd.clear();
          lcd.print("Modo Vigilancia");
          Serial.println("PUERTA CERRADA: Volviendo a vigilancia.");
          presenciaDetectada = false;
        } else {
          // Actuadores en caso de fallo
          lcd.clear();
          lcd.print("PIN Incorrecto");
          Serial.println("BRECHA DE SEGURIDAD: Intento fallido.");
          delay(2000);
          lcd.clear();
          lcd.print("Ingrese PIN:");
          claveIngresada = "";
        }
      } else if (tecla == '*') {
        claveIngresada = ""; // Tecla para borrar
        lcd.setCursor(0, 1);
        lcd.print("                ");
      } else {
        claveIngresada += tecla;
        lcd.setCursor(0, 1);
        lcd.print(claveIngresada);
      }
    }
  }
}

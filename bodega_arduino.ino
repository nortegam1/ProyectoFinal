#include <Servo.h>

const int SENS[3] = {2, 3, 4};
const int VERDE[3] = {5, 6, 7};
const int ROJO[3] = {8, 9, 10};
const int LED_ALARMA = 11;
const int BUZZER = 12;
const int PIN_SERVO = A0;
const int BOTON = A1;

const bool SENSOR_ACTIVO_BAJO = true;

Servo servo;
char estado[3] = {'L', 'L', 'L'};
bool presente[3];
int cont[3] = {0, 0, 0};
bool alarma = false;
bool abierto = false;
bool botonAnt = false;

bool leer(int i) {
  int v = digitalRead(SENS[i]);
  return SENSOR_ACTIVO_BAJO ? (v == LOW) : (v == HIGH);
}

void actualizarSalidas() {
  bool abrir = false;
  for (int i = 0; i < 3; i++) {
    digitalWrite(VERDE[i], estado[i] == 'L' ? HIGH : LOW);
    digitalWrite(ROJO[i], estado[i] == 'L' ? LOW : HIGH);
    if (estado[i] == 'U') {
      abrir = true;
    }
  }
  digitalWrite(LED_ALARMA, alarma ? HIGH : LOW);
  digitalWrite(BUZZER, alarma ? HIGH : LOW);
  if (abrir != abierto) {
    servo.write(abrir ? 90 : 0);
    abierto = abrir;
  }
}

void leerSerial() {
  while (Serial.available()) {
    String l = Serial.readStringUntil('\n');
    l.trim();
    if (l.startsWith("ESTADO:") && l.length() >= 10) {
      int n = l.charAt(7) - '0';
      if (n >= 1 && n <= 3) {
        estado[n - 1] = l.charAt(9);
      }
    } else if (l.startsWith("ALARMA:") && l.length() >= 8) {
      alarma = l.charAt(7) == '1';
    }
    actualizarSalidas();
  }
}

void setup() {
  Serial.begin(9600);
  for (int i = 0; i < 3; i++) {
    pinMode(SENS[i], INPUT);
    pinMode(VERDE[i], OUTPUT);
    pinMode(ROJO[i], OUTPUT);
    presente[i] = leer(i);
  }
  pinMode(LED_ALARMA, OUTPUT);
  pinMode(BUZZER, OUTPUT);
  pinMode(BOTON, INPUT_PULLUP);
  servo.attach(PIN_SERVO);
  servo.write(0);
  actualizarSalidas();
  Serial.println("LISTO");
}

void loop() {
  for (int i = 0; i < 3; i++) {
    bool lect = leer(i);
    if (lect == presente[i]) {
      cont[i] = 0;
    } else if (++cont[i] >= 5) {
      cont[i] = 0;
      presente[i] = lect;
      if (!lect && estado[i] == 'A') {
        alarma = true;
        Serial.print("MOV:");
        Serial.println(i + 1);
        actualizarSalidas();
      }
    }
  }

  bool b = digitalRead(BOTON) == LOW;
  if (b && !botonAnt) {
    Serial.println("BTN");
  }
  botonAnt = b;

  leerSerial();
  delay(20);
}
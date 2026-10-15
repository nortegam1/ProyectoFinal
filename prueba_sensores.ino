const int SENS[3] = {2, 3, 4};
const bool SENSOR_ACTIVO_BAJO = true;

void setup() {
  Serial.begin(9600);
  for (int i = 0; i < 3; i++) {
    pinMode(SENS[i], INPUT);
  }
}

void loop() {
  for (int i = 0; i < 3; i++) {
    int v = digitalRead(SENS[i]);
    bool presente = SENSOR_ACTIVO_BAJO ? (v == LOW) : (v == HIGH);
    Serial.print("S");
    Serial.print(i + 1);
    Serial.print(presente ? "=PAQUETE   " : "=vacio     ");
  }
  Serial.println();
  delay(300);
}
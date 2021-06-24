#define F_CPU 16000000L
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <string.h>
#include <Wire.h> 
#include <LiquidCrystal_I2C.h>

#define set_bit(value, bit) ( _SFR_BYTE(value) |= _BV(bit) )
#define clear_bit(value, bit) ( _SFR_BYTE(value) &= ~_BV(bit) )
#define DEBUG true

LiquidCrystal_I2C lcd(0x27,16,2);  // set the LCD address to 0x27 for a 16 chars and 2 line display

void Initial(void);
void Init_wifi(void);
void sendData(String command, const int timeout, boolean debug);

volatile int timer_count = 0;
volatile bool accFlag = false;
volatile uint8_t state = 1;

int main(void){
  String income_wifi = "";
  String income_bluetooth = "";
  String income_usb = "";
  String wifi_temp ="";
  
  int index = 0;
  int process_data = 0;
  char buff[25] = "";
  char lati[25] = "";
  char longi[25] = "";
  char data;
  int wf = 0;

  int buzzerPin = 52;
    
  init();               //초기화
  lcd.init();           //LCD초기화
  lcd.backlight();
  Initial();            //register setting
  Init_wifi();          //wifi setting
  sei();                //interrupt enable

  pinMode(buzzerPin, OUTPUT);   //부저핀 설정
  lcd.print("START CAR03");
  
  while(1){

    
    //usb -> wifi
    if(Serial.available()){
      Serial2.write(Serial.read());
    }

    if(Serial2.available()){
      Serial.write(Serial2.read());
    }
    
    if(Serial1.available()){
      data = Serial1.read();
      if(data == 'n' || data == 'e'){
        if(data == 'n') wf=1;
        else wf=2;
        process_data = 1;
      }
      else if(data == '\r'){
        process_data = 2;
      }
      if(process_data == 1){
        buff[index] = data;
        index++;
      }
      else if(process_data == 2){
        if(wf == 1) {
          strcpy(lati,buff);
          lati[index] = '\r';
          lati[index+1] = '\n';
        }
        else if(wf == 2){
          strcpy(longi,buff);
          longi[index] = '\r';
          longi[index+1] = '\n';
          Serial.println(lati);
          Serial.println(longi);
          accFlag = true;
        }
        index = 0;
        process_data = 0;
        wf = 0;
      }
    }
   
    if(accFlag == true){
       //사고시 동작
      Serial2.println(lati);
      Serial2.println(longi);
      Serial.println(lati);
      Serial.println(longi);
      
      digitalWrite(buzzerPin, HIGH);
      delay(3000);
      digitalWrite(buzzerPin, LOW);
      
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("ACC");
      lcd.print(lati);
      lcd.setCursor(0,1);
      lcd.print(longi);
      sendData("AT+CIPSTART=\"TCP\",\"192.168.2.1\",8082\r\n",5000,DEBUG);
      accFlag = false;
    }
    else{
      //평소 동작
      if(timer_count == 5){
          timer_count = 0;
      }
    }
    
  }
  return 0;
}

//=========================== register setting
void Initial(void){
  // uart0 (20,21)(컴퓨터)
  Serial.begin(115200);                 //baud rate
  Serial.setTimeout(100);              //최대 5초까지 응답 대기
  Serial.println("Micom_3 Start\n");    //Micom_n Start
  Serial.setTimeout(100);              //최대 5초까지 응답 대기

  // uart1 (18,19)(wifi)
  Serial1.begin(115200);                //baud rate
  Serial1.setTimeout(100);             //최대 5초까지 응답 대기

  // uart2 (16,17)(bluetooth)
  Serial2.begin(115200);                //baud rate
  Serial2.setTimeout(100);             //최대 5초까지 응답 대기

  // INT4
  EIMSK |= (1 << INT4);                 //INT4 enable
  EICRB |= (1 << ISC41);                //Falling edge interrupt 
  clear_bit(DDRE, 1);                   //PORTE 입력
  set_bit(PORTE, 1);                    //풀업 저항
  
  // timer/counter 1 
  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1 = 0;
  OCR1A = 15625;                        //0.000064s*15625=1s 주기 (MAX=65536)
  TCCR1B |= (1 << WGM12);               //CTC mode
  TCCR1B |= (1 << CS12);                //256 prescalar, 16M/256=62,500, 1clock=0.000016s
  TCCR1B |= (1 << CS10);                //1024 prescalar, 16M/1024=15,625, 1clock=0.000064s
  TIMSK1 |= (1 << OCIE1A);              //enable timer compare interrupt  
}

//=========================== wifi_setting
void Init_wifi(void){
  //sendData(command, timeout, bebug)
  //CAR-0n, 192.168.10n.1, 800n
  sendData("AT+RST\r\n",5000,DEBUG);                                  // reset module
  sendData("AT+CWMODE=1\r\n",5000,DEBUG);                             // configure as access point, <MODE_1=station, 2=AP, 3=station+AP>
  sendData("AT+CWJAP=\"CAR-02\",\"1234test\"\r\n",10000,DEBUG);
  sendData("AT+CIPSTART=\"TCP\",\"192.168.2.1\",8082\r\n",5000,DEBUG);
}

//=========================== Timer/Counter1
ISR(TIMER1_COMPA_vect){
   timer_count++;
}

ISR(INT4_vect){
//  accFlag = true;
//  Serial.println("accFlag_ON");
}

//=========================== send data by wifi
void sendData(String command, const int timeout, boolean debug) {
  String response = "";
  Serial1.print(command); // send the read character to the wifi
  long int time = millis();
  while( (time+timeout) > millis()) {
    while(Serial1.available()) {  // The esp has data so display its output to the serial window 
      char c = Serial1.read(); // read the next character.
      response+=c;
    }
  }
  if(debug) Serial.print(response);
  return 0;
}

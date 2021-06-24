#define F_CPU 16000000L
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <Time.h>
#include <math.h>

#define set_bit(value, bit) ( _SFR_BYTE(value) |= _BV(bit) )
#define clear_bit(value, bit) ( _SFR_BYTE(value) &= ~_BV(bit) )
#define DEBUG true

void Inital(void);
void Init_wifi(void);
String sendData(String command, const int timeout, boolean debug);

volatile int count = 0;
volatile uint8_t state = 1;

struct Information {
  char ID[3];               //car ID
  int AC = 0;                //Accident Count
  double latitude = 0;        //위도
  double longitude = 0;       //경도
}info;

int main(void){
  String income_wifi;
  String income_usb;
  String wifi_temp;
  char rec_temp;
  struct tm timeinfo;
  
  init();               //초기화
  //setTime(hr,min,sec,day,month,yr);//setTime(hr,min,sec,day,month,yr);
  Initial();            //register setting
  Init_wifi();          //wifi setting
  TIMSK1 |= (1 << OCIE1A);              //enable timer compare interrupt 
  sei();                //interrupt enable

  set_bit(DDRB, 7);
  
  while(1){
    income_wifi = "";
    income_usb = "";
    wifi_temp = "CAR";

    if(!state) set_bit(PORTB, 7);
    else clear_bit(PORTB, 7);
    
    //wifi -> usb
    if(Serial1.available()){              //return recieve data length(byte)
      Serial.print("wifi_receive\n");
      Serial.setTimeout(1000);
      rec_temp = Serial1.read();
      income_wifi += rec_temp;
      Serial.print(income_wifi);
      Serial.setTimeout(1000);
    }
    
    //usb -> wifi
    if(Serial.available()){
      Serial2.write(Serial.read());
    }

    if(Serial2.available()){
      Serial.print(Serial2.read());
    }
  }
  return 0;
}

//=========================== register setting
void Initial(void){
  // uart0 (20,21)(컴퓨터)
  Serial.begin(115200);                 //baud rate
  Serial.setTimeout(5000);              //최대 5초까지 응답 대기
  Serial.println("Micom_2 Start\n");    //Micom_n Start
  Serial.setTimeout(5000);              //최대 5초까지 응답 대기

  // uart1 (18,19)(wifi)
  Serial1.begin(115200);                //baud rate
  Serial1.setTimeout(5000);             //최대 5초까지 응답 대기

  // uart2 (16,17)(bluetooth)
  Serial2.begin(115200);                //baud rate
  Serial2.setTimeout(5000);             //최대 5초까지 응답 대기

  // INT4
  EIMSK |= (1 << INT4);                 //INT4 enable
  EICRB |= (1 << ISC41);                //Falling edge interrupt 
  clear_bit(DDRE, 1);                   //PORTE 입력
  set_bit(PORTE, 1);                    //풀업 저항
  
  // timer/counter 1 
  TCCR1A = 0;
  TCCR1B = 0;
  TCNT1 = 0;
  OCR1A = 22500;                        //0.000016s*22500=0.36s 주기
  TCCR1B |= (1 << WGM12);               //CTC mode
  TCCR1B |= (1 << CS12);                //256 prescalar, 16M/256=62500, 1clock=0.000016s
  TIMSK1 |= (1 << OCIE1A);              //enable timer compare interrupt  
}

//=========================== wifi_setting
void Init_wifi(void){
  //sendData(command, timeout, bebug)
  //CAR-0n, 192.168.10n.1, 808n
  sendData("AT+RST\r\n",2000,DEBUG);                                  // reset module
  sendData("AT+CWMODE=3\r\n",1000,DEBUG);                             // configure as access point, <MODE_1=station, 2=AP, 3=station+AP>
  sendData("AT+CWSAP=\"CAR-02\",\"1234test\",11,3\r\n",1000,DEBUG);   // join the access point <"ssid","pwd","chl","ecn"> <ecn_0:open, 1:wpa_psk, 2:wpa2_psk, 3:wpa_wpa2__psk>
  sendData("AT+CIPMUX=1\r\n",1000,DEBUG);                             // configure for multiple connections, <0:single, 2:multiple connnection>
  sendData("AT+CIPAP=\"192.168.102.1\",\"192.168.102.1\",\"255.255.255.0\r\n",1000,DEBUG); // configure for multiple connections, <0:single, 2:multiple connnection>
  sendData("AT+CIPSERVER=1,8082\r\n",1000,DEBUG);                     // turn on server on port 80
  sendData("AT+CIFSR\r\n",1000,DEBUG);                                // get ip address
}

//=========================== Timer/Counter1
ISR(TIMER1_COMPA_vect){
   Serial.print(".");
   Serial.setTimeout(5000);  //최대 5초까지 응답 대기
   count++;
   if(count == 50){
    Serial.println("");
    Serial.setTimeout(150);
    count = 0;
  }
}

ISR(INT4_vect){
  state = !state;
  Serial.println("acc");
  Serial.setTimeout(150);
}
//=========================== send data by wifi
String sendData(String command, const int timeout, boolean debug) {
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
  return response;
}

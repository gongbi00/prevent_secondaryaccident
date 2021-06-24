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
  char test[5] = "car";
  char rec_temp;
  struct tm timeinfo;
  
  init();               //초기화
  //setTime(hr,min,sec,day,month,yr);//setTime(hr,min,sec,day,month,yr);
  Initial();            //register setting
  Init_wifi();          //wifi setting
  //TIMSK1 |= (1 << OCIE1A);              //enable timer compare interrupt
  sei();                //interrupt enable

  set_bit(DDRB, 7);
  
  while(1){
    income_wifi = "";
    income_usb = "";
    //wifi_temp = "";
    String test = "test";
    if(!state) set_bit(PORTB, 7);
    else clear_bit(PORTB, 7);

    if(count == 10){
      sendData("AT+CWLAP\r\n",100,DEBUG);    //AP검색
      count = 0;
    }
    
    //wifi -> usb
    if(Serial1.available()){              //return recieve data length(byte)
      if(Serial1.find("CAR-0")){
        income_wifi = Serial1.readStringUntil('"');
        wifi_temp = income_wifi.substring(0,1);
        wifi_temp += "test";
        Serial.println(wifi_temp);
      }
      //rec_temp = Serial1.read();
      //income_wifi += rec_temp;
      //Serial.print(income_wifi);
      //Serial.setTimeout(100);
    }
    
    //usb -> wifi
    if(Serial.available()){
      Serial2.write(Serial.read());
    }

    if(Serial2.available()){
      Serial.write(Serial2.read());
    }
  }
  return 0;
}

//=========================== register setting
void Initial(void){
  // uart0 (20,21)(컴퓨터)
  Serial.begin(115200);                 //baud rate
  Serial.setTimeout(100);              //최대 5초까지 응답 대기
  Serial.println("Micom_1 Start\n");    //Micom_n Start
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
  OCR1A = 65535;                        //0.000064s*65535=4.19424s 주기 (MAX=65536)
  TCCR1B |= (1 << WGM12);               //CTC mode
  TCCR1B |= (1 << CS12);                //256 prescalar, 16M/256=62,500, 1clock=0.000016s
  TCCR1B |= (1 << CS10);                //1024 prescalar, 16M/1024=15,625, 1clock=0.000064s
  TIMSK1 |= (1 << OCIE1A);              //enable timer compare interrupt  
}

//=========================== wifi_setting
void Init_wifi(void){
  //sendData(command, timeout, bebug)
  //CAR-0n, 192.168.10n.1, 800n
  sendData("AT+RST\r\n",1000,DEBUG);                                  // reset module
  sendData("AT+CWMODE=3\r\n",1000,DEBUG);                             // configure as access point, <MODE_1=station, 2=AP, 3=station+AP>
  sendData("AT+CWSAP=\"CAR-01\",\"1234test\",11,3\r\n",1000,DEBUG);   // join the access point <"ssid","pwd","chl","ecn"> <ecn_0:open, 1:wpa_psk, 2:wpa2_psk, 3:wpa_wpa2__psk>
  sendData("AT+CIPMUX=1\r\n",1000,DEBUG);                             // configure for multiple connections, <0:single, 2:multiple connnection>
  sendData("AT+CIPAP=\"192.168.101.1\",\"192.168.101.1\",\"255.255.255.0\r\n",1000,DEBUG); // configure for multiple connections, <0:single, 2:multiple connnection>
  sendData("AT+CIPSERVER=1,8001\r\n",1000,DEBUG);                     // turn on server on port 80
  sendData("AT+CIFSR\r\n",1000,DEBUG);                                // get ip address
}

//=========================== Timer/Counter1
ISR(TIMER1_COMPA_vect){
   count++;
   //Serial1.print("AT+CWLAP\r\n");
   //Serial1.setTimeout(5000);
   //sendData("AT+CWLAP\r\n",100,DEBUG);                        //AP검색
}
/*
ISR(INT4_vect){
  //연결설정
  sendData("AT+CWJAP=\"CAR-02\",\"1234test\"\r\n",500,DEBUG);          //AP연결(respose:WIFI CONNECTED, WIFI GOT IP)
  sendData("AT+CIPSTART=\"TCP\",\"192.168.102.1\",8082",500,DEBUG);    //TCP/IP연결(response:CONNET OK)
  //data 전송
  sendData("AT+CIPSEND=6",500,DEBUG);                                   //sendData길이+1(respose:OK)
  sendData("hello",500,DEBUG);                                           //전송할 데이터 입력(response:SEND OK)
  //연결끊기
  sendData("AT+CIPCLOSE",500,DEBUG);                                   //연결종료(respose:CLOSED OK)
}
*/
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

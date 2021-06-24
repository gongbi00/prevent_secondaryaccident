#define F_CPU 16000000L
#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <string.h>
#include <Time.h>
#include <math.h>

#define set_bit(value, bit) ( _SFR_BYTE(value) |= _BV(bit) )
#define clear_bit(value, bit) ( _SFR_BYTE(value) &= ~_BV(bit) )
#define DEBUG true

void Initial(void);
void Init_wifi(void);
String sendData(String command, const int timeout, boolean debug);

volatile int timer_count = 0;
volatile bool accINTFlag = false;
volatile uint8_t state = 1;

int main(void){
  init();               //초기화
  Initial();            //register setting
  Init_wifi();          //wifi setting
  sei();                //interrupt enable

  while(1){
    String lati = "33.4546340\r\n";
    String longi = "126.562996\r\n";
    String income_wifi = "";
    String income_bluetooth = "";
    String income_usb = "";
    
    //usb -> wifi
    if(Serial.available()){
      Serial1.write(Serial.read());
    }

    if(Serial2.available()){
      Serial.write(Serial2.read());
    }

     if(Serial1.available()){
      char inChar = (char)Serial1.read();
      income_wifi += inChar;
      
    }

    //충돌시 동작
    if(accINTFlag == true){
      for(int i=0; i<5; i++){
        String sendLen = "\"AT+CIPSEND=";
        sendLen += (char)i;
        sendLen += ",11\r\n\"";
        Serial.println(sendLen);
        sendData(sendLen, 5000, DEBUG);
        sendData(lati,5000,DEBUG);
        sendData(longi,5000,DEBUG);
      }
      /*
      sendData("AT+CIPSEND=0,10\r\n",5000,DEBUG);
      sendData(lati,5000,DEBUG);
      sendData("AT+CIPSEND=0,11\r\n",5000,DEBUG);
      sendData(longi,5000,DEBUG);
      
      sendData("AT+CIPSEND=1,10\r\n",5000,DEBUG);
      sendData(lati,5000,DEBUG);
      sendData("AT+CIPSEND=1,11\r\n",5000,DEBUG);
      sendData(longi,5000,DEBUG);

      sendData("AT+CIPSEND=2,10\r\n",5000,DEBUG);
      sendData(lati,5000,DEBUG);
      sendData("AT+CIPSEND=2,11\r\n",5000,DEBUG);
      sendData(longi,5000,DEBUG);

      sendData("AT+CIPSEND=3,10\r\n",5000,DEBUG);
      sendData(lati,5000,DEBUG);
      sendData("AT+CIPSEND=3,11\r\n",5000,DEBUG);
      sendData(longi,5000,DEBUG);
      */
      accINTFlag = false;
    }
    else{
      //평소 동작
      if(timer_count == 2){
          //sendData("AT+CWLAP\r\n",5000,DEBUG);
          //sendData("AT+CWJAP=\"CAR-01\",\"1234test\"\r\n",20000,DEBUG);
          //sendData("AT+CIFSR\r\n",10000,DEBUG);
          //sendData("AT+CIPSTART=\"TCP\",\"192.168.1.1\",8081\r\n",10000,DEBUG);
          sendData("AT+CWLAP\r\n",5000,DEBUG);
          sendData("AT+CIPSEND=12\r\n",5000,DEBUG);
          sendData("from CAR-02\r\n",5000,DEBUG);
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
  sendData("AT+CWMODE=3\r\n",5000,DEBUG);                             // configure as access point, <MODE_1=station, 2=AP, 3=station+AP>
  sendData("AT+CWSAP=\"CAR-01\",\"1234test\",11,3\r\n",5000,DEBUG);   // join the access point <"ssid","pwd","chl","ecn"> <ecn_0:open, 1:wpa_psk, 2:wpa2_psk, 3:wpa_wpa2__psk>
  sendData("AT+CIPMUX=1\r\n",5000,DEBUG);                             // configure for multiple connections, <0:single, 2:multiple connnection>
  sendData("AT+CIPAP=\"192.168.1.1\",\"192.168.1.1\",\"255.255.255.0\r\n",1000,DEBUG); // configure for multiple connections, <0:single, 2:multiple connnection>
  sendData("AT+CIPSERVER=1,8081\r\n",5000,DEBUG);                     // turn on server on port 80
}


  
//=========================== Timer/Counter1
ISR(TIMER1_COMPA_vect){
   timer_count++;
}

ISR(INT4_vect){
  accINTFlag = true;
  Serial.println("accINTFlag_ON");
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

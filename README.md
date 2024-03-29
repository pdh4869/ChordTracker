![image](https://github.com/pdh4869/ChordTracker/assets/76561901/753ee598-c7f9-41e6-a3a0-bc89d46841ac)![image](https://github.com/pdh4869/ChordTracker/assets/76561901/04f8f8b5-70c7-481c-a363-7d16e240e334)![image](https://github.com/pdh4869/ChordTracker/assets/76561901/eb5f1279-a1e8-4a55-aa08-43edbd67095a)# 악기 연주자들을 위한 음계 인식 애플리케이션 (ChordTracker for Instrument Players)

## 문제 상황
1. 기존의 음계 인식 애플리케이션은 정확성이 떨어짐
2. 특정 장비의 가격이 다소 고가이고, 사용 범위가 한정적임 (특정 회사의 악기만 인식 가능) 

## 구현 계획 
1. 스마트폰 마이크 인식 기능 구현 
2. 녹음된 음성 파일 스마트폰 저장소에 저장 
3. 녹음한 소리로부터 일치하는 음계를 화면에 출력
4. 도출된 음계를 악보로 활용할 수 있도록 디자인
5. 클라우드에 저장할 수 있도록 업로드 기능 구현

## 사용 효과
1. 표현된 음계를 통해 작곡 등에 활용함
2. 대중가요를 copy하여 악기를 연습할 수 있도록 함
3. 악기를 조율하는 데 활용할 수 있음

## 사용자 요구사항
1. 소리 인식이 잘 되어야 함 
2. 도출된 음계를 텍스트화 하여 악보로 활용 가능해야 함
3. PC 환경이 익숙한 사용자들을 위해 클라우드 등에 저장할 수 있어야 함

## 시스템 요구사항
1. 단시간 푸리에 변환 알고리즘 사용(STFT) - FFT를 사용하는 다른 라이브러리보다 비교적 명확한 그래프가 나오는 TarsosDSP 라이브러리 사용  
2. 도출된 음계를 활용하여 악보를 디자인 할 수 있도록 구현함
3. Google Drive API 사용 (클라우드 업로드)

## 상용화된 비슷한 앱과 비교한 개선점
1. 연속적인 음의 처리가 가능하도록 함
2. 별도의 장치 없이 스마트폰만 활용하여 금전적 부담이 없도록 함

## 개선할 점
1. 악보 디자인 개선
2. Drive 업로드 기능 마무리 
3. 잡음 개선 코드 추가 


## 과정
1. STFT
![image](https://github.com/pdh4869/ChordTracker/assets/76561901/4d9fdab5-3821-4242-9b8e-9e49287a9ba4) ![image](https://github.com/pdh4869/ChordTracker/assets/76561901/67e95d1c-3b25-4b08-8dc3-266738d73505)

2. Pitch Detecting
![image](https://github.com/pdh4869/ChordTracker/assets/76561901/19c9d059-3c9f-402d-ac79-4788aaf555c9)
![image](https://github.com/pdh4869/ChordTracker/assets/76561901/9bb355e2-1526-48e9-9405-c9aa106ce683)
![image](https://github.com/pdh4869/ChordTracker/assets/76561901/b395b9d7-a2e6-4f77-ba64-0e0e0255c619)


3. 결과
![image](https://github.com/pdh4869/ChordTracker/assets/76561901/dbc4ff43-417d-4585-8548-fa6e21636840)
![image](https://github.com/pdh4869/ChordTracker/assets/76561901/48607887-d630-406d-85e6-86240ed5b653)





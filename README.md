# KobertBasedKotlinDateParser
KoBERT로 DT / TI 태그가 붙은 단어들을 활용해
시작 시간과 종료 시간을 구할 수 있는 코틀린 코드를 구상해보고 있습니다

# 입력 형태 가정 : 머신러닝 처리 결과물 json을 <단어-태그> 묶음 리스트로 가공
건축가       O
<건축가      O
명사특강>    O
2019.8.27   DT_OTHERS
-9.24       DT_OTHERS
어디서      O
살          O
것인가      O
9.3         DT_OTHERS
180*        O
매주        DT_OTHERS
(화)        DT_OTHERS
16:00       TI_HOUR
-           O
18:00       TI_HOUR
세종시청     O
4층         O
여민실       O
정태인       O
|           O

# 목표 : 일정 리스트를 추출해서 어플로 넘김
* 시작일과 종료일을 가진 구조체에 담을 생각

>> <scheduleItem> 폴더 설명
  - 단계적으로 감싸지는 구조체
  - 최종목표 ItemSchedule에 시작일과 종료일을 담을 것임
  - 시작일과 종료일 DT(년월일)와 TI(시분)을 가진 공통점을 따 같은 구조체에 이름만 from과 to로 하는데, 그 구조체 이름이 ItemSide
  - ItemSide 안에 DT정보를 가진 ItemDate, TI정보를 가진 ItemTime이 있음
  - 분해 순서로 ItemSchedule -> ItemSide 2개
               ItemSide -> ItemDate 하나, ItemTime 하나
               ItemDate 안에는 int형 3개로 연, 월, 일 가지고 있음
               ItemTime 안에는 int형 2개로 시, 분 가지고 있음
  
  - 각 구조체의 range는 정보가 입력 형태에서 몇번 째부터 몇번째 단어까지에서 구한 정보인지 저장함(예: 2번째 단어는 [ <건축가 ])
  - nullable(구조체 변수에 물음표붙이는거) 처리는 메인 코드 작성하면서 필요한대로 다룬 거라 설계 근거는 부실한 게 맞습니다
  - ItemTime의 inserted:Boolean은 ItemSide를 만들 때, 
     DT관련 영역을 처리할 때 따라 들어간 TI와 그렇지 못한 TI를 구분해 시간만 가지고도 일정을 만들 의도입니다
 
별도 폴더로 있는 StringPositionRecorder는
처음에 DT가 연속으로 등장하면 붙이는 작업을 하는데,
여러 단어를 합치는 작업인데 이 단계부터 이미 이 단어가 몇번째부터 몇번째 단어다 범위를 추적해야
후의 작업들에 범위를 적용해서 작업할 수 있기 때문에 그걸 위해 맨 처음 쓰이는 구조체입니다
                                                                              
안 쓰는 파일들은 곧 정리해두겠습니다 

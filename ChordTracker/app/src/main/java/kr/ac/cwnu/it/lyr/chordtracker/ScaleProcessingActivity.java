package kr.ac.cwnu.it.lyr.chordtracker;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.writer.WriterProcessor;

public class ScaleProcessingActivity extends AppCompatActivity {

    TarsosDSPAudioFormat tarsosDSPAudioFormat; // fft 관련
    TextView pitchText; // 주파수 (hz) 텍스트 표현
    AudioDispatcher dispatcher; // 오디오 녹음, 재생에 사용
    File file; // 오디오 파일로 만들기
    Button btn_record1, btn_play1; // 녹음, 재생 버튼
    String totalScale = "scale: "; // 주파수, 주파수에 따른 음계 화면에 나타내기
    ImageView imageView;

    public static String saveStorage = ""; // 기기 내 텍스트 파일(totalScale) 저장 경로
    public static String saveData = ""; // totalScale(주파수에 따른 음계) 저장될 내용

    boolean isRecording = false; // 녹음 여부 확인
    String filename = "recorded_sound.wav"; // 기기 내 저장될 음성 파일 이름

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scaleprocessing);

        File sdCard = Environment.getExternalStorageDirectory(); // 기기 내 sd카드 경로
        file = new File(sdCard, filename);

        /*
        filePath = file.getAbsolutePath();
        Log.e("MainActivity", "저장 파일 경로 :" + filePath); // 저장 파일 경로 : /storage/emulated/0/recorded.mp3
        */

        tarsosDSPAudioFormat=new TarsosDSPAudioFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
                22050,
                2 * 8,
                1,
                2 * 1,
                22050,
                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()));
        // sample rate, sample size in bits, channels(1: mono, 2:stereo), frame size, frame rate 순

        pitchText = findViewById(R.id.pitchText);
        btn_record1 = findViewById(R.id.btn_record1);
        btn_play1 = findViewById(R.id.btn_play1);
        imageView = findViewById(R.id.imageView);

        btn_record1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRecording) // 녹음 시작
                {
                    recordAudio();
                    isRecording = true;
                    btn_record1.setText("Stop");
                    imageView.setImageResource(R.drawable.piano);
                }
                else // 녹음 끝
                {
                    stopRecording();
                    isRecording = false;
                    btn_record1.setText("Record");
                    saveText(totalScale); // 주파수에 따른 음계  > 텍스트 파일로 생성 및 저장
                    imageView.setImageResource(R.drawable.piano);
                }
            }
        });

        btn_play1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio();
                imageView.setImageResource(R.drawable.piano);
            }
        });  // 재생버튼

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } // 마이크 허용 부분 (없으면 사용 불가능, 에러 발생)
    }

    public void saveText(String data) {
        try {
            saveData = data; // totalScale (주파수에 따른 음계) 들어가는 변수
            String textFileName = "/lyrics.txt"; // 텍스트 파일 이름 설정. 아래는 저장할 경로
            File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()); // 디렉토리 경로

            if (!storageDir.exists()) {
                storageDir.mkdir(); // 저장할 디렉터리가 없으면 새로 생성
            }
            long now = System.currentTimeMillis();  // 현재 날짜, 시간 등 저장
            Date date = new Date(now);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
            String nowTime = sdf.format(date);

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(storageDir+textFileName, false));
            bufferedWriter.append("["+nowTime+"]" + "\n["+data+"]");
            bufferedWriter.newLine();
            bufferedWriter.close();

            saveStorage = String.valueOf(storageDir+textFileName); // 경로 저장

            // 잘 저장되었는지 확인
            Log.d("---","---");
            Log.w("//===========//","================================================");
            Log.d("start","\n"+"[A_TextFile > 저장한 텍스트 파일 확인 실시]");
            Log.d("","\n"+"[경로 : "+String.valueOf(saveStorage)+"]");
            Log.d("","\n"+"[제목 : "+String.valueOf(nowTime)+"]");
            Log.d("end","\n"+"[내용 : "+String.valueOf(saveData)+"]");
            Log.w("//===========//","================================================");
            Log.d("---","---");

            Toast.makeText(getApplicationContext(), "텍스트 파일 저장 완료",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void playAudio()
    {
        try{
            releaseDispatcher(); // 오디오 녹음 끝나면 dispatcher -> stop
            // 오디오 파일 재생
            FileInputStream fileInputStream = new FileInputStream(file);
            dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, tarsosDSPAudioFormat), 1024, 0);
            AudioProcessor playerProcessor = new AndroidAudioPlayer(tarsosDSPAudioFormat, 2048, 0);
            dispatcher.addAudioProcessor(playerProcessor);
            // 주파수 pitch detecting을 통한 음계를 찾기 위한 변수
            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e){
                    final double pitchInHz = (Math.round(res.getPitch() * 100) / 100.0); // 주파수 pitch 저장 변수

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pitchInHz>= 30 && pitchInHz <= 32) {    // 1옥타브
                                pitchText.setText("C1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 33 && pitchInHz <= 35) {
                                pitchText.setText("C#1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 36 && pitchInHz <= 37) {
                                pitchText.setText("D1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 38 && pitchInHz <= 40) {
                                pitchText.setText("D#1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 41 && pitchInHz <= 42) {
                                pitchText.setText("E1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 43 && pitchInHz <= 44) {
                                pitchText.setText("F1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 45 && pitchInHz <= 47) {
                                pitchText.setText("F#1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 48 && pitchInHz <= 50) {
                                pitchText.setText("G1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 51 && pitchInHz <= 53) {
                                pitchText.setText("G#1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 54 && pitchInHz <= 56) {
                                pitchText.setText("A1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 57 && pitchInHz <= 59) {
                                pitchText.setText("A#1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 60 && pitchInHz <= 63) {
                                pitchText.setText("B1 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 64 && pitchInHz <= 67) {  // 2옥타브
                                pitchText.setText("C2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 68 && pitchInHz <= 72) {
                                pitchText.setText("C#2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 73 && pitchInHz <= 76) {
                                pitchText.setText("D2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 77 && pitchInHz <= 81) {
                                pitchText.setText("D#2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 82 && pitchInHz <= 86) {
                                pitchText.setText("E2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 87 && pitchInHz <= 91) {
                                pitchText.setText("F2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 92 && pitchInHz <= 96) {
                                pitchText.setText("F#2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 97 && pitchInHz <= 101) {
                                pitchText.setText("G2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 102 && pitchInHz <= 107) {
                                pitchText.setText("G#2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 108 && pitchInHz <= 114) {
                                pitchText.setText("A2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 115 && pitchInHz <= 121) {
                                pitchText.setText("A#2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 122 && pitchInHz <= 126) {
                                pitchText.setText("B2 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 127 && pitchInHz <= 134) { // 3옥타브
                                pitchText.setText("C3 - "+pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 135 && pitchInHz <= 142) {
                                pitchText.setText("C#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 143 && pitchInHz <= 150) {
                                pitchText.setText("D3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 151 && pitchInHz <= 159) {
                                pitchText.setText("D#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 160 && pitchInHz <= 168) {
                                pitchText.setText("E3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 169 && pitchInHz <= 176) {
                                pitchText.setText("F3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 177 && pitchInHz <= 190) {
                                pitchText.setText("F#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 191 && pitchInHz <= 203) {
                                pitchText.setText("G3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 204 && pitchInHz <= 215) {
                                pitchText.setText("G#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 216 && pitchInHz <= 227) {
                                pitchText.setText("A3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 228 && pitchInHz <= 240) {
                                pitchText.setText("A#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 241 && pitchInHz <= 255) {
                                pitchText.setText("B3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 256 && pitchInHz <= 269) { // 4옥타브
                                pitchText.setText("C4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 270 && pitchInHz <= 285) {
                                pitchText.setText("C#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 286 && pitchInHz <= 303) {
                                pitchText.setText("D4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 304 && pitchInHz <= 320) {
                                pitchText.setText("D#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 321 && pitchInHz <= 339) {
                                pitchText.setText("E4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 340 && pitchInHz <= 359) {
                                pitchText.setText("F4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 360 && pitchInHz <= 380) {
                                pitchText.setText("F#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 381 && pitchInHz <= 402) {
                                pitchText.setText("G4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 403 && pitchInHz <= 430) {
                                pitchText.setText("G#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 431 && pitchInHz <= 458) {
                                pitchText.setText("A4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 459 && pitchInHz <= 487) {
                                pitchText.setText("A#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 488 && pitchInHz <= 516) {
                                pitchText.setText("B4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 517 && pitchInHz <= 546) { // 5옥타브
                                pitchText.setText("C5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 547 && pitchInHz <= 573) {
                                pitchText.setText("C#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 574 && pitchInHz <= 607) {
                                pitchText.setText("D5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 608 && pitchInHz <= 642) {
                                pitchText.setText("D#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 643 && pitchInHz <= 682) {
                                pitchText.setText("E5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 683 && pitchInHz <= 726) {
                                pitchText.setText("F5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 727 && pitchInHz <= 770) {
                                pitchText.setText("F#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 771 && pitchInHz <= 815) {
                                pitchText.setText("G5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 816 && pitchInHz <= 865) {
                                pitchText.setText("G#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 866 && pitchInHz <= 920) {
                                pitchText.setText("A5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 921 && pitchInHz <= 974) {
                                pitchText.setText("A#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 975 && pitchInHz <= 1033) {
                                pitchText.setText("B5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 1034 && pitchInHz <= 1090) { // 6옥타브
                                pitchText.setText("C6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 1091 && pitchInHz <= 1155) {
                                pitchText.setText("C#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 1156 && pitchInHz <= 1220) {
                                pitchText.setText("D6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 1221 && pitchInHz <= 1290) {
                                pitchText.setText("D#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 1291 && pitchInHz <= 1365) {
                                pitchText.setText("E6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 1366 && pitchInHz <= 1450) {
                                pitchText.setText("F6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 1451 && pitchInHz <= 1535) {
                                pitchText.setText("F#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 1536 && pitchInHz <= 1630) {
                                pitchText.setText("G6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 1631 && pitchInHz <= 1720) {
                                pitchText.setText("G#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 1721 && pitchInHz <= 1830) {
                                pitchText.setText("A6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 1831 && pitchInHz <= 1940) {
                                pitchText.setText("A#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 1941 && pitchInHz <= 2050) {
                                pitchText.setText("B6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 2051 && pitchInHz <= 2160) { // 7옥타브
                                pitchText.setText("C7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 2161 && pitchInHz <= 2290) {
                                pitchText.setText("C#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 2291 && pitchInHz <= 2441) {
                                pitchText.setText("D7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 2442 && pitchInHz <= 2590) {
                                pitchText.setText("D#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 2591 && pitchInHz <= 2741) {
                                pitchText.setText("E7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 2742 && pitchInHz <= 2900) {
                                pitchText.setText("F7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 2901 && pitchInHz <= 3061) {
                                pitchText.setText("F#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 3062 && pitchInHz <= 3262) {
                                pitchText.setText("G7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 3263 && pitchInHz <= 3462) {
                                pitchText.setText("G#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 3463 && pitchInHz <= 3662) {
                                pitchText.setText("A7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 3663 && pitchInHz <= 3870) {
                                pitchText.setText("A#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>= 3871 && pitchInHz <= 4100) {
                                pitchText.setText("B7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            } else if (pitchInHz>= 4101 && pitchInHz <= 4350) { // 8옥타브
                                pitchText.setText("C8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                            } else if (pitchInHz>= 4351 && pitchInHz <= 4600) {
                                pitchText.setText("C#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                            } else if (pitchInHz>= 4601 && pitchInHz <= 4880) {
                                pitchText.setText("D8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz>= 4881 && pitchInHz <= 5170) {
                                pitchText.setText("D#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz>= 5171 && pitchInHz <= 5480) {
                                pitchText.setText("E8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                            } else if (pitchInHz>= 5481 && pitchInHz <= 5800) {
                                pitchText.setText("F8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                            } else if (pitchInHz>= 5801 && pitchInHz <= 6150) {
                                pitchText.setText("F#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz>= 6151 && pitchInHz <= 6520) {
                                pitchText.setText("G8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                            } else if (pitchInHz>= 6521 && pitchInHz <= 6900) {
                                pitchText.setText("G#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz>= 6901 && pitchInHz <= 7300) {
                                pitchText.setText("A8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                            } else if (pitchInHz>= 7301 && pitchInHz <= 7750) {
                                pitchText.setText("A#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                            } else if (pitchInHz>=7751) {
                                pitchText.setText("B8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                            }
                        }
                    });
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor); // 주파수 pitch 찾기

            Thread audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start(); // 오디오 재생
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void recordAudio()
    {
        releaseDispatcher();
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);

        try { // 1번째 줄, totalScale += 제외 playAudio() 파일과 설명 같음
            RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rw"); // 파일 읽고 쓰기
            AudioProcessor recordProcessor = new WriterProcessor(tarsosDSPAudioFormat, randomAccessFile);
            dispatcher.addAudioProcessor(recordProcessor);

            PitchDetectionHandler pitchDetectionHandler = new PitchDetectionHandler() {
                @Override
                public void handlePitch(PitchDetectionResult res, AudioEvent e){
                    final double pitchInHz = (Math.round(res.getPitch() * 100) / 100.0); // 주파수 pitch 저장 변수
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pitchInHz >= 30 && pitchInHz <= 32) {    // 1옥타브
                                pitchText.setText("C1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C1";
                            } else if (pitchInHz >= 33 && pitchInHz <= 35) {
                                pitchText.setText("C#1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#1";
                            } else if (pitchInHz >= 36 && pitchInHz <= 37) {
                                pitchText.setText("D1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D1";
                            } else if (pitchInHz >= 38 && pitchInHz <= 40) {
                                pitchText.setText("D#1 - " + pitchInHz);
                                totalScale += "D#1";
                                imageView.setImageResource(R.drawable.d_sharp);
                            } else if (pitchInHz >= 41 && pitchInHz <= 42) {
                                pitchText.setText("E1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E1";
                            } else if (pitchInHz >= 43 && pitchInHz <= 44) {
                                pitchText.setText("F1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F1";
                            } else if (pitchInHz >= 45 && pitchInHz <= 47) {
                                pitchText.setText("F#1 - " + pitchInHz);
                                totalScale += "F#1";
                                imageView.setImageResource(R.drawable.f_sharp);
                            } else if (pitchInHz >= 48 && pitchInHz <= 50) {
                                pitchText.setText("G1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G1";
                            } else if (pitchInHz >= 51 && pitchInHz <= 53) {
                                pitchText.setText("G#1 - " + pitchInHz);
                                totalScale += "G#1";
                                imageView.setImageResource(R.drawable.g_sharp);
                            } else if (pitchInHz >= 54 && pitchInHz <= 56) {
                                pitchText.setText("A1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A1";
                            } else if (pitchInHz >= 57 && pitchInHz <= 59) {
                                pitchText.setText("A#1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#1";
                            } else if (pitchInHz >= 60 && pitchInHz <= 63) {
                                pitchText.setText("B1 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B1";
                            } else if (pitchInHz >= 64 && pitchInHz <= 67) {  // 2옥타브
                                pitchText.setText("C2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C2";
                            } else if (pitchInHz >= 68 && pitchInHz <= 72) {
                                pitchText.setText("C#2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#2";
                            } else if (pitchInHz >= 73 && pitchInHz <= 76) {
                                pitchText.setText("D2 - " + pitchInHz);
                                totalScale += "D2";
                                imageView.setImageResource(R.drawable.d);
                            } else if (pitchInHz >= 77 && pitchInHz <= 81) {
                                pitchText.setText("D#2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#2";
                            } else if (pitchInHz >= 82 && pitchInHz <= 86) {
                                pitchText.setText("E2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E2";
                            } else if (pitchInHz >= 87 && pitchInHz <= 91) {
                                pitchText.setText("F2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F2";
                            } else if (pitchInHz >= 92 && pitchInHz <= 96) {
                                pitchText.setText("F#2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#2";
                            } else if (pitchInHz >= 97 && pitchInHz <= 101) {
                                pitchText.setText("G2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G2";
                            } else if (pitchInHz >= 102 && pitchInHz <= 107) {
                                pitchText.setText("G#2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#2";
                            } else if (pitchInHz >= 108 && pitchInHz <= 114) {
                                pitchText.setText("A2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A2";
                            } else if (pitchInHz >= 115 && pitchInHz <= 121) {
                                pitchText.setText("A#2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#2";
                            } else if (pitchInHz >= 122 && pitchInHz <= 126) {
                                pitchText.setText("B2 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B2";
                            } else if (pitchInHz >= 127 && pitchInHz <= 134) { // 3옥타브
                                pitchText.setText("C3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C3";
                            } else if (pitchInHz >= 135 && pitchInHz <= 142) {
                                pitchText.setText("C#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#3";
                            } else if (pitchInHz >= 143 && pitchInHz <= 150) {
                                pitchText.setText("D3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D3";
                            } else if (pitchInHz >= 151 && pitchInHz <= 159) {
                                pitchText.setText("D#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#3";
                            } else if (pitchInHz >= 160 && pitchInHz <= 168) {
                                pitchText.setText("E3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E3";
                            } else if (pitchInHz >= 169 && pitchInHz <= 176) {
                                pitchText.setText("F3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F3";
                            } else if (pitchInHz >= 177 && pitchInHz <= 190) {
                                pitchText.setText("F#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#3";
                            } else if (pitchInHz >= 191 && pitchInHz <= 203) {
                                pitchText.setText("G3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G3";
                            } else if (pitchInHz >= 204 && pitchInHz <= 215) {
                                pitchText.setText("G#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#3";
                            } else if (pitchInHz >= 216 && pitchInHz <= 227) {
                                pitchText.setText("A3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A3";
                            } else if (pitchInHz >= 228 && pitchInHz <= 240) {
                                pitchText.setText("A#3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#3";
                            } else if (pitchInHz >= 241 && pitchInHz <= 255) {
                                pitchText.setText("B3 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B3";
                            } else if (pitchInHz >= 256 && pitchInHz <= 269) { // 4옥타브
                                pitchText.setText("C4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C4";
                            } else if (pitchInHz >= 270 && pitchInHz <= 285) {
                                pitchText.setText("C#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#4";
                            } else if (pitchInHz >= 286 && pitchInHz <= 303) {
                                pitchText.setText("D4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D4";
                            } else if (pitchInHz >= 304 && pitchInHz <= 320) {
                                pitchText.setText("D#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#4";
                            } else if (pitchInHz >= 321 && pitchInHz <= 339) {
                                pitchText.setText("E4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E4";
                            } else if (pitchInHz >= 340 && pitchInHz <= 359) {
                                pitchText.setText("F4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F4";
                            } else if (pitchInHz >= 360 && pitchInHz <= 380) {
                                pitchText.setText("F#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#4";
                            } else if (pitchInHz >= 381 && pitchInHz <= 402) {
                                pitchText.setText("G4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G4";
                            } else if (pitchInHz >= 403 && pitchInHz <= 430) {
                                pitchText.setText("G#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#4";
                            } else if (pitchInHz >= 431 && pitchInHz <= 458) {
                                pitchText.setText("A4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A4";
                            } else if (pitchInHz >= 459 && pitchInHz <= 487) {
                                pitchText.setText("A#4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#4";
                            } else if (pitchInHz >= 488 && pitchInHz <= 516) {
                                pitchText.setText("B4 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B4";
                            } else if (pitchInHz >= 517 && pitchInHz <= 546) { // 5옥타브
                                pitchText.setText("C5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C5";
                            } else if (pitchInHz >= 547 && pitchInHz <= 573) {
                                pitchText.setText("C#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#5";
                            } else if (pitchInHz >= 574 && pitchInHz <= 607) {
                                pitchText.setText("D5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D5";
                            } else if (pitchInHz >= 608 && pitchInHz <= 642) {
                                pitchText.setText("D#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#5";
                            } else if (pitchInHz >= 643 && pitchInHz <= 682) {
                                pitchText.setText("E5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E5";
                            } else if (pitchInHz >= 683 && pitchInHz <= 726) {
                                pitchText.setText("F5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F5";
                            } else if (pitchInHz >= 727 && pitchInHz <= 770) {
                                pitchText.setText("F#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#5";
                            } else if (pitchInHz >= 771 && pitchInHz <= 815) {
                                pitchText.setText("G5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G5";
                            } else if (pitchInHz >= 816 && pitchInHz <= 865) {
                                pitchText.setText("G#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#5";
                            } else if (pitchInHz >= 866 && pitchInHz <= 920) {
                                pitchText.setText("A5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A5";
                            } else if (pitchInHz >= 921 && pitchInHz <= 974) {
                                pitchText.setText("A#5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#5";
                            } else if (pitchInHz >= 975 && pitchInHz <= 1033) {
                                pitchText.setText("B5 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B5";
                            } else if (pitchInHz >= 1034 && pitchInHz <= 1090) { // 6옥타브
                                pitchText.setText("C6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C6";
                            } else if (pitchInHz >= 1091 && pitchInHz <= 1155) {
                                pitchText.setText("C#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#6";
                            } else if (pitchInHz >= 1156 && pitchInHz <= 1220) {
                                pitchText.setText("D6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D6";
                            } else if (pitchInHz >= 1221 && pitchInHz <= 1290) {
                                pitchText.setText("D#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#6";
                            } else if (pitchInHz >= 1291 && pitchInHz <= 1365) {
                                pitchText.setText("E6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E6";
                            } else if (pitchInHz >= 1366 && pitchInHz <= 1450) {
                                pitchText.setText("F6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F6";
                            } else if (pitchInHz >= 1451 && pitchInHz <= 1535) {
                                pitchText.setText("F#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#6";
                            } else if (pitchInHz >= 1536 && pitchInHz <= 1630) {
                                pitchText.setText("G6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G6";
                            } else if (pitchInHz >= 1631 && pitchInHz <= 1720) {
                                pitchText.setText("G#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#6";
                            } else if (pitchInHz >= 1721 && pitchInHz <= 1830) {
                                pitchText.setText("A6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A6";
                            } else if (pitchInHz >= 1831 && pitchInHz <= 1940) {
                                pitchText.setText("A#6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#6";
                            } else if (pitchInHz >= 1941 && pitchInHz <= 2050) {
                                pitchText.setText("B6 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B6";
                            } else if (pitchInHz >= 2051 && pitchInHz <= 2160) { // 7옥타브
                                pitchText.setText("C7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C7";
                            } else if (pitchInHz >= 2161 && pitchInHz <= 2290) {
                                pitchText.setText("C#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#7";
                            } else if (pitchInHz >= 2291 && pitchInHz <= 2441) {
                                pitchText.setText("D7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D7";
                            } else if (pitchInHz >= 2442 && pitchInHz <= 2590) {
                                pitchText.setText("D#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#7";
                            } else if (pitchInHz >= 2591 && pitchInHz <= 2741) {
                                pitchText.setText("E7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E7";
                            } else if (pitchInHz >= 2742 && pitchInHz <= 2900) {
                                pitchText.setText("F7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F7";
                            } else if (pitchInHz >= 2901 && pitchInHz <= 3061) {
                                pitchText.setText("F#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#7";
                            } else if (pitchInHz >= 3062 && pitchInHz <= 3262) {
                                pitchText.setText("G7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G7";
                            } else if (pitchInHz >= 3263 && pitchInHz <= 3462) {
                                pitchText.setText("G#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#7";
                            } else if (pitchInHz >= 3463 && pitchInHz <= 3662) {
                                pitchText.setText("A7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A7";
                            } else if (pitchInHz >= 3663 && pitchInHz <= 3870) {
                                pitchText.setText("A#7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#7";
                            } else if (pitchInHz >= 3871 && pitchInHz <= 4100) {
                                pitchText.setText("B7 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B7";
                            } else if (pitchInHz >= 4101 && pitchInHz <= 4350) { // 8옥타브
                                pitchText.setText("C8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c);
                                totalScale += "C8";
                            } else if (pitchInHz >= 4351 && pitchInHz <= 4600) {
                                pitchText.setText("C#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.c_sharp);
                                totalScale += "C#8";
                            } else if (pitchInHz >= 4601 && pitchInHz <= 4880) {
                                pitchText.setText("D8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d);
                                totalScale += "D8";
                            } else if (pitchInHz >= 4881 && pitchInHz <= 5170) {
                                pitchText.setText("D#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.d_sharp);
                                totalScale += "D#8";
                            } else if (pitchInHz >= 5171 && pitchInHz <= 5480) {
                                pitchText.setText("E8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.e);
                                totalScale += "E8";
                            } else if (pitchInHz >= 5481 && pitchInHz <= 5800) {
                                pitchText.setText("F8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f);
                                totalScale += "F8";
                            } else if (pitchInHz >= 5801 && pitchInHz <= 6150) {
                                pitchText.setText("F#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.f_sharp);
                                totalScale += "F#8";
                            } else if (pitchInHz >= 6151 && pitchInHz <= 6520) {
                                pitchText.setText("G8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g);
                                totalScale += "G8";
                            } else if (pitchInHz >= 6521 && pitchInHz <= 6900) {
                                pitchText.setText("G#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.g_sharp);
                                totalScale += "G#8";
                            } else if (pitchInHz >= 6901 && pitchInHz <= 7300) {
                                pitchText.setText("A8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a);
                                totalScale += "A8";
                            } else if (pitchInHz >= 7301 && pitchInHz <= 7750) {
                                pitchText.setText("A#8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.a_sharp);
                                totalScale += "A#8";
                            } else if (pitchInHz >= 7751) {
                                pitchText.setText("B8 - " + pitchInHz);
                                imageView.setImageResource(R.drawable.b);
                                totalScale += "B8";
                            }
                        }
                    });
                }
            };

            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, pitchDetectionHandler);
            dispatcher.addAudioProcessor(pitchProcessor);

            Thread audioThread = new Thread(dispatcher, "Audio Thread");
            audioThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording()
    {
        releaseDispatcher();
    }

    public void releaseDispatcher()
    {
        if(dispatcher != null)
        {
            if(!dispatcher.isStopped())
                dispatcher.stop();
            dispatcher = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseDispatcher();
    }
}


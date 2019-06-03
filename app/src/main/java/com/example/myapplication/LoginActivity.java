package com.example.myapplication;

import android.app.AppComponentFactory;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // 레이아웃 : visible, gone
    private LinearLayout mLayoutLogin;
    private LinearLayout mLayoutSignup;

    // 상태값 : 로그인여부, 상세정보
    private String mStatus;
    private String mDetail;

    // 로그인 : 이메일 패스워드
    private EditText mEmailFieldLogin;
    private EditText mPasswordFieldLogin;

    // 계정생성 : 이메일 패스워드
    private EditText mNameFieldSignUp;
    private EditText mEmailFieldSignUp;
    private EditText mPasswordFieldSIgnUp;

    // 링크 : 레이아웃변경
    private TextView mLinkLogin;
    private TextView mLinkSignUp;

    // 버튼
    private Button mBtnLogin;
    private Button mBtnSignUp;

    // [START declare_auth]
    //private FirebaseAuth mAuth;
    // [END declare_auth]

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        initView();

        setListener();

//    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//    imm.hideSoftInputFromWindow(mEmailFiled.getWindowToken(), 0);

    }

    public void initView() {

        // 레이아웃 : visible, gone
        mLayoutLogin = (LinearLayout) findViewById(R.id.layout_login);
        mLayoutSignup = (LinearLayout) findViewById(R.id.layout_signup);

        // 로그인 : 이메일 패스워드
        mEmailFieldLogin = (EditText) findViewById(R.id.loginFieldEmail);
        mPasswordFieldLogin = (EditText) findViewById(R.id.loginFieldPassword);

        // 계정생성 : 이메일 패스워드
        mNameFieldSignUp = (EditText) findViewById(R.id.createFieldName);
        mEmailFieldSignUp = (EditText) findViewById(R.id.createFieldEmail);
        mPasswordFieldSIgnUp = (EditText) findViewById(R.id.createFieldPassword);

        // 링크 : 레이아웃변경
        mLinkLogin = (TextView) findViewById(R.id.link_login);
        mLinkSignUp = (TextView) findViewById(R.id.link_signup);

        // 버튼 : 로그인, 계정생성 메소드 진행
        mBtnLogin = (Button) findViewById(R.id.emailSignInButton);
        mBtnSignUp = (Button) findViewById(R.id.emailCreateAccountButton);

        // [START initialize_auth]
        // Initialize Firebase Auth
        // mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

    }


    public void setListener() {

        // 로그인 화면 생성
        mLinkLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutLogin.setVisibility(View.VISIBLE);
                mLayoutSignup.setVisibility(View.GONE);
            }
        });

        // 회원가입 화면 생성
        mLinkSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutLogin.setVisibility(View.GONE);
                mLayoutSignup.setVisibility(View.VISIBLE);
            }
        });

        // 로그인
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signIn(mEmailFieldLogin.getText().toString(), mPasswordFieldLogin.getText().toString());


            }
        });

        // 계정생성
        mBtnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                createAccount(mNameFieldSignUp.getText().toString(),
                        mEmailFieldSignUp.getText().toString(),
                        mPasswordFieldSIgnUp.getText().toString());

            }
        });

    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 로그인 되있으면 그냥 넘어감
        updateUI(currentUser);
    }
    // [END on_start_check_user]


    private void createAccount(final String mNameFiled, String mEmailFiled, String mPasswordField) {
        Log.d(TAG, "createAccount:" + mEmailFiled);

        if (!validateFormSignUp()) {
            return;
        }
        showProgressDialog();
        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(mEmailFiled, mPasswordField)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // 프로필 업데이트 (이름 추가)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(mNameFiled)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User profile updated.");
                                            } else {
                                                Log.d(TAG, "User profile not updated.");
                                            }
                                        }

                                    });

                            // 이메일 인증 메일 전송
                            sendEmailVerification();

                            // Firestore 호출
                            initFirestore();

                            // FIrestore에 customers 추가
                            addFirestoreCustomer(user, mNameFiled);

                            // 다음 진행
                            updateUI(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "이미 가입되어있거나, 인증에 실패했습니다.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END create_user_with_email]
    }

    private void addFirestoreCustomer(FirebaseUser user, String userNickname) {

        Customer customer = new Customer();
        customer.setInitUser(user, userNickname);

        // Get a reference to the restaurants collection
        CollectionReference customers = mFirestore.collection("customers");
        DocumentReference doc = customers.document(user.getUid());
        doc.set(customer);

    }

    private void signIn(String mEmailFiled, String mPasswordField) {
        Log.d(TAG, "signIn:" + mEmailFiled);

        // 유효하지 않은 형식이면 return
        if (!validateFormLogin()) {
            return;
        }

        // 진행바
        showProgressDialog();

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(mEmailFiled, mPasswordField)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            // 로그인 생성
                            Log.d(TAG, "signInWithEmail:success");
                            // 이름 불러와서 환영인사 해주기

                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "인증이 실패하였습니다.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            mStatus = "Authentication failed";
                        }
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END sign_in_with_email]
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private boolean validateFormSignUp() {
        boolean valid = true;

        String name = mNameFieldSignUp.getText().toString();
        if (TextUtils.isEmpty(name)) {
            mNameFieldSignUp.setError("Required.");
            valid = false;
        } else {
            mNameFieldSignUp.setError(null);
        }

        String email = mEmailFieldSignUp.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailFieldSignUp.setError("Required.");
            valid = false;
        } else {
            mEmailFieldSignUp.setError(null);
        }

        String password = mPasswordFieldSIgnUp.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordFieldSIgnUp.setError("Required.");
            valid = false;
        } else {
            mPasswordFieldSIgnUp.setError(null);
        }

        return valid;
    }

    private boolean validateFormLogin() {
        boolean valid = true;

        String email = mEmailFieldLogin.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailFieldLogin.setError("Required.");
            valid = false;
        } else {
            mEmailFieldLogin.setError(null);
        }

        String password = mPasswordFieldLogin.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordFieldLogin.setError("Required.");
            valid = false;
        } else {
            mPasswordFieldLogin.setError(null);
        }

        return valid;
    }

    private void sendEmailVerification() {
        // Disable button
        //findViewById(R.id.verifyEmailButton).setEnabled(false);

        // Send verification email
        // [START send_email_verification]
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // [START_EXCLUDE]
                        // Re-enable button
                        //findViewById(R.id.verifyEmailButton).setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "회원가입 인증메일이 전송되었습니다. " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "sendEmailVerification", task.getException());

                        }
                    }
                });
    }


    private void updateUI (FirebaseUser user){
        hideProgressDialog();

        // 계정 생성 성공 또는 로그인 성공
        if (user != null) {

            // mStatus = getString(R.string.emailpassword_status_fmt) +
            //          + user.getEmail() + user.isEmailVerified();

            // detail.setText(getString(R.string.firebase_status_fmt, user.getUid()));
            mDetail = user.getUid();

            // 이름 불러와서 환영인사 해주기
            Toast.makeText(this, "방문을 환영합니다", Toast.LENGTH_SHORT).show();

            // 액티비티 실행
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        }
        // 계정 생성실패, 로그인 실패
        else {

            // 실패 문구 날리기
            // Toast.makeText(this, "로그인 또는 계정생성 실패", Toast.LENGTH_SHORT).show();

        }
    }


}




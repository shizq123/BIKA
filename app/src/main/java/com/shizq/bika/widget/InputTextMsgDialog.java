package com.shizq.bika.widget;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;

import com.shizq.bika.R;

public class InputTextMsgDialog extends AppCompatDialog {
    private final Context mContext;
    private TextView titleView;
    private EditText editView;
    private Button btn_send;
    private LinearLayout rldlgview;
    private int mLastDiff = 0;

    public interface OnTextSendListener { void onTextSend(String msg);}
    private OnTextSendListener mOnTextSendListener;

    public InputTextMsgDialog(@NonNull Context context) {
        super(context, R.style.InputTextMsgDialog);
        this.mContext = context;

        this.getWindow().setWindowAnimations(R.style.InputTextMsgDialog_anim);
        initView();
        initListener();
        setLayout();
    }

    /**
     * 设置输入提示文字
     */
    public void setHint(String text) {
        editView.setHint(text);
    }

    /**
     * 设置按钮的文字  默认为：发送
     */
    public void setBtnText(String text) {
        btn_send.setText(text);
    }

    public void setTitleText(String text) {
        titleView.setText(text);
    }

    private void initView() {
        setContentView(R.layout.dialog_input_text_msg);
        titleView =findViewById(R.id.dialog_input_title);//输入框
        editView =findViewById(R.id.dialog_input_edit);//输入框
        rldlgview = findViewById(R.id.dialog_input_layout);
        btn_send = findViewById(R.id.dialog_input_btn_send);

        //弹出软键盘
        editView.setFocusableInTouchMode(true);
        editView.requestFocus();
    }

    private void initListener() {
        btn_send.setEnabled(false);//默认关
        editView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                btn_send.setEnabled(editable.length() != 0);
            }
        });

        btn_send.setOnClickListener(view -> {
            String msg = editView.getText().toString();
            if (!TextUtils.isEmpty(msg)) {
                mOnTextSendListener.onTextSend(msg);
                editView.setText("");
                dismiss();
            } else {
                Toast.makeText(mContext, "请输入文字", Toast.LENGTH_LONG).show();
            }
            editView.setText(null);
        });

        editView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case KeyEvent.KEYCODE_ENDCALL:
                    case KeyEvent.KEYCODE_ENTER:

                        if (editView.getText().length() > 0) {
                            mOnTextSendListener.onTextSend(editView.getText().toString());
                            editView.setText("");
                            dismiss();
                        } else {
                            Toast.makeText(mContext, "请输入文字", Toast.LENGTH_LONG).show();
                        }
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        dismiss();
                        return false;
                    default:
                        return false;
                }
            }
        });

        rldlgview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                Rect r = new Rect();
                //获取当前界面可视部分
                getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
                //获取屏幕的高度
                int screenHeight = getWindow().getDecorView().getRootView().getHeight();
                //此处就是用来获取键盘的高度的， 在键盘没有弹出的时候 此高度为0 键盘弹出的时候为一个正数
                int heightDifference = screenHeight - r.bottom;

                if (heightDifference <= 0 && mLastDiff > 0) {
                    dismiss();
                }
                mLastDiff = heightDifference;
            }
        });

    }

    private void setLayout() {
        getWindow().setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams p = getWindow().getAttributes();
        p.width = WindowManager.LayoutParams.MATCH_PARENT;
        p.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(p);
    }


    public void setmOnTextSendListener(OnTextSendListener onTextSendListener) {
        this.mOnTextSendListener = onTextSendListener;
    }

    @Override
    public void dismiss() {
        //dismiss之前重置mLastDiff值避免下次无法打开
        mLastDiff = 0;
        super.dismiss();
    }

}

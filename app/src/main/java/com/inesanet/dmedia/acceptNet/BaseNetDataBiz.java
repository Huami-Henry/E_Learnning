package com.inesanet.dmedia.acceptNet;
import android.os.Handler;
import android.os.Message;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
/**
 * Created by Administrator on 2016/4/15 0015.
 */
public class BaseNetDataBiz {
    public BaseNetDataBiz(){}
    private RequestListener listener;
    public BaseNetDataBiz(RequestListener listener) {
        this.listener = listener;
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            listener.onResponse((Model) msg.obj);
        }
    };
    /**
     * @param tag
     */
    public void downloadFile(String url, final String tag) {
        OkHttp.asyncPost(url,tag,new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                listener.OnFailure(request, e);
            }
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    Model model = new Model();
                    model.setTag((String) response.request().tag());
                    model.setJson(json);
                    Message message = new Message();
                    message.obj = model;
                    handler.sendMessage(message);
                }
            }
        });
    }
    public interface RequestListener{
        void onResponse(Model model);
        void OnFailure(Request r, IOException o);
    }
    public class Model{
        private String tag;
        private String json;
        private ResponseBody body;

        public ResponseBody getBody() {
            return body;
        }

        public void setBody(ResponseBody body) {
            this.body = body;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getJson() {
            return json;
        }

        public void setJson(String json) {
            this.json = json;
        }
    }
}

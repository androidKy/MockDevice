package com.quin.backup.util;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * description:
 * author: kyXiao
 * created date: 2018/9/13
 */

public class CommandUtil {

    public static void sendCommand(String command) {
        sendCommand(command, null);
    }

    public static void sendCommand(String command, OnResponListener responListener) {
        String result = execute(command);
        if (result != null) {
            if (result.contains("\n")) {
                String[] splits = result.split("\n");

                List<String> dataList = new ArrayList<>(splits.length);
                dataList.addAll(Arrays.asList(splits));
                responListener.onSuccess(dataList);
            } else {
                List<String> dataList = new ArrayList<>(1);
                dataList.add(result);
                responListener.onSuccess(dataList);
            }
        } else {
            List<String> dataList = new ArrayList<>();
            responListener.onSuccess(dataList);
        }
       /* List<String> dataList = new ArrayList<>();
        DataOutputStream dataOutputStream = null;
        DataInputStream dataInputStream = null;
        DataInputStream dataErrorInputStream = null;
        Process process = null;
        StringBuilder errorLine = new StringBuilder();
        try {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());

            dataOutputStream.writeBytes(command + "\n");
            dataOutputStream.flush();
            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();

            dataErrorInputStream = new DataInputStream(process.getErrorStream());
            dataInputStream = new DataInputStream(process.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(dataInputStream));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(dataErrorInputStream));

            String line = reader.readLine();
            Log.d(TAG, "command line:" + line);
            String line_error;
            for (line_error = errReader.readLine(); line != null; line = reader.readLine()) {
                if (!TextUtils.isEmpty(line))
                    dataList.add(line);

                if (!TextUtils.isEmpty(line_error))
                    errorLine.append(line_error).append("\n");
            }

            if (responListener != null) {
                if (dataList.size() > 0 || TextUtils.isEmpty(errorLine.toString())) {
                    responListener.onSuccess(dataList);
                } else if (!TextUtils.isEmpty(errorLine.toString()))
                    responListener.onFailed(errorLine.toString());
            }
           // int exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            responListener.onFailed(e.getMessage());
        } finally {
            closeStream(dataInputStream);
            closeStream(dataOutputStream);
            closeStream(dataErrorInputStream);
            if (process != null) {
                process.destroy();
            }
        }*/
    }

    private static void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnResponListener {
        void onSuccess(List<String> responList);

        void onFailed(String msg);
    }


    //换行符
    private static final String BREAK_LINE;
    //执行退出命令
    private static final byte[] COMMAND_EXIT;
    //错误缓冲
    private static byte[] BUFFER;

    /**
     * 静态变量初始化
     */
    static {
        BREAK_LINE = "\n";
        COMMAND_EXIT = "\nexit\n".getBytes();
        BUFFER = new byte[32];
    }


    /**
     * 执行命令
     *
     * @param params 命令参数
     *               <pre> eg: "/system/bin/ping", "-c", "4", "-s", "100","www.qiujuer.net"</pre>
     * @return 执行结果
     */
    public static String execute(String params) {
        Process process = null;
        StringBuilder sbReader = null;

        BufferedReader bReader = null;
        InputStreamReader isReader = null;

        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;

        try {
            /*process = new ProcessBuilder("su")
                    .command(params)
                    .start();*/
            ProcessBuilder pb = new ProcessBuilder("su");
            pb.redirectErrorStream(true);
            process = pb.start();

            out = process.getOutputStream();
            in = process.getInputStream();
            err = process.getErrorStream();

            out.write(params.getBytes());
            out.flush();
            out.write(COMMAND_EXIT);
            out.flush();

            process.waitFor();

            isReader = new InputStreamReader(in);
            bReader = new BufferedReader(isReader);

            String s;
            if ((s = bReader.readLine()) != null) {
                sbReader = new StringBuilder();
                sbReader.append(s);
                sbReader.append(BREAK_LINE);
                while ((s = bReader.readLine()) != null) {
                    sbReader.append(s);
                    sbReader.append(BREAK_LINE);
                }
            }

           /* while ((err.read(BUFFER)) > 0) {

            }*/
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeAllStream(out, err, in, isReader, bReader);

            if (process != null) {
                processDestroy(process);
            }
        }

        if (sbReader == null)
            return null;
        else
            return sbReader.toString();
    }

    /**
     * 关闭所有流
     *
     * @param out      输出流
     * @param err      错误流
     * @param in       输入流
     * @param isReader 输入流封装
     * @param bReader  输入流封装
     */
    private static void closeAllStream(OutputStream out, InputStream err, InputStream in, InputStreamReader isReader, BufferedReader bReader) {
        if (out != null)
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (err != null)
            try {
                err.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (in != null)
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (isReader != null)
            try {
                isReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (bReader != null)
            try {
                bReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }


    /**
     * 通过Android底层实现进程关闭
     *
     * @param process 进程
     */
    private static void killProcess(Process process) {
        int pid = getProcessId(process);
        if (pid != 0) {
            try {
                //android kill process
                android.os.Process.killProcess(pid);
            } catch (Exception e) {
                try {
                    process.destroy();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * 获取进程的ID
     *
     * @param process 进程
     * @return
     */
    private static int getProcessId(Process process) {
        String str = process.toString();
        try {
            int i = str.indexOf("=") + 1;
            int j = str.indexOf("]");
            str = str.substring(i, j);
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 销毁进程
     *
     * @param process 进程
     */
    private static void processDestroy(Process process) {
        if (process != null) {
            try {
                //判断是否正常退出
                if (process.exitValue() != 0) {
                    killProcess(process);
                }
            } catch (IllegalThreadStateException e) {
                killProcess(process);
            }
        }
    }
}

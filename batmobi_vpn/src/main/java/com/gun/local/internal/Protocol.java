package com.gun.local.internal;

/**
 * description:协议值
 * author: diff
 * date: 2017/12/28.
 */
public interface Protocol {
    byte REGISTER_A = 101;
    byte REGISTER_B = 102;
    byte HEART_A = 103;
    byte HEART_B = 104;
    byte HEART_RESPONSE_A = 105;
    byte HEART_RESPONSE_B = 106;
    byte PAIR_RESULT = 107;
    byte CLOSE_A_B = 108;
    byte CLOSE_SERVER_B = 109;
}

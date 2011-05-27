/*
 * $Id: $
 */
package org.microsoft.security.ntlm.impl;

import static org.microsoft.security.ntlm.impl.Algorithms.ByteArray;
import static org.microsoft.security.ntlm.impl.Algorithms.UNICODE_ENCODING;
import static org.microsoft.security.ntlm.impl.Algorithms.bytesTo2;
import static org.microsoft.security.ntlm.impl.Algorithms.bytesTo4;
import static org.microsoft.security.ntlm.impl.Algorithms.compareArray;

/**
 * [MS-NLMP]
 *
 * 2.2.1.2 CHALLENGE_MESSAGE
The CHALLENGE_MESSAGE defines an NTLM challenge message that is sent from the server to the
client. The CHALLENGE_MESSAGE is used by the server to challenge the client to prove its identity.
For connection-oriented requests, the CHALLENGE_MESSAGE generated by the server is in response
to the NEGOTIATE_MESSAGE (section 2.2.1.1) from the client.


CHALLENGE_MESSAGE
TargetNameFields [12..20]
NegotiateFlags [20..24]
ServerChallenge [24..22]
Reserved [22..30]
TargetInfoFields [30..38]
Version [38..46]
Payload [46..]: TargetNameBufferOffset, TargetInfoBufferOffset

 *
 * @author <a href="http://profiles.google.com/109977706462274286343">Veritatem Quaeres</a>
 * @version $Revision: $
 */
public class NtlmChallengeMessage {

    private byte[] messageData;
    private int negotiateFlags;
    private ByteArray serverChallenge;
    private ByteArray targetInfo;
    private ByteArray time;
    private NtlmRoutines.MsvAvFlag msvAvFlag;

    public NtlmChallengeMessage(byte[] data) {
        messageData = data;

        // Signature (8 bytes): An 8-byte character array that MUST contain the ASCII string ('N', 'T', 'L', 'M', 'S', 'S', 'P', '\0').

        if (!compareArray(data, 0, NtlmRoutines.NTLM_MESSAGE_SIGNATURE, 0, NtlmRoutines.NTLM_MESSAGE_SIGNATURE.length)) {
            throw new RuntimeException("Invalid signature");
        }

        // [8..12] MessageType (4 bytes): A 32-bit unsigned integer that indicates the message type. This field MUST be set to 0x00000002.
        int messageType = bytesTo4(data, 8);
        if (messageType != 2) {
            throw new RuntimeException("Invalid message type: " + messageType);
        }

        // [12..20] TargetNameFields (8 bytes): If the NTLMSSP_REQUEST_TARGET flag is set in NegotiateFlags, indicating that TargetName is required:
        // TargetNameLen, TargetNameMaxLen, and TargetNameBufferOffset


        // [20..24] NegotiateFlags (4 bytes)
        negotiateFlags = bytesTo4(data, 20);

        /*
        [24..32] ServerChallenge (8 bytes): A 64-bit value that contains the NTLM challenge. The challenge is
        a 64-bit nonce. The processing of the ServerChallenge is specified in sections 3.1.5 and 3.2.5.
        */
        serverChallenge = new ByteArray(data, 24, 8);

        // [32..40] Reserved (8 bytes): An 8-byte array whose elements MUST be zero when sent and MUST be ignored on receipt.

        // [40..48] TargetInfoFields (8 bytes): If the NTLMSSP_NEGOTIATE_TARGET_INFO flag of NegotiateFlags is set, indicating that TargetInfo is required
        // TargetInfoLen, TargetInfoMaxLen, and TargetInfoBufferOffset

        /*
        [48..56]
Version (8 bytes): A VERSION structure (as defined in section 2.2.2.10) that is present only
when the NTLMSSP_NEGOTIATE_VERSION flag is set in the NegotiateFlags field. This
structure is used for debugging purposes only. In normal (non-debugging) protocol messages,
it is ignored and does not affect the NTLM message processing.<7>

<7> Section 2.2.1.2: The Version field is NOT sent or accessed by Windows NT or Windows 2000.
Windows NT and Windows 2000 assume that the Payload field started immediately after
TargetInfoBufferOffset. Since all references into the Payload field are by offset from the start of
the message (not from the start of the Payload field), Windows NT and Windows 2000 can correctly
interpret messages with Version fields.

         */


        if (NtlmRoutines.NTLMSSP_NEGOTIATE_TARGET_INFO.isSet(negotiateFlags)) {
            parseTargetInfo();
        }
    }

    public byte[] getMessageData() {
        return messageData;
    }

    public ByteArray getServerChallenge() {
        return serverChallenge;
    }

    public ByteArray getTime() {
        return time;
    }

    public ByteArray getTargetInfo() {
        return targetInfo;
    }

    public NtlmRoutines.MsvAvFlag getMsvAvFlag() {
        return msvAvFlag;
    }

    public int getNegotiateFlags() {
        return negotiateFlags;
    }

    /**
     * TargetInfoFields (8 bytes): If the NTLMSSP_NEGOTIATE_TARGET_INFO flag of
NegotiateFlags is set, indicating that TargetInfo is required:
     */
    private void parseTargetInfo() {
        targetInfo = NtlmRoutines.getMicrosoftArray(messageData, 40);

        int offset = targetInfo.getOffset();
        while (true) {
            if (offset >= targetInfo.getOffset() + targetInfo.getLength()) {
                throw new RuntimeException("Target info out of bound: " + offset);
            }
            int id = bytesTo2(messageData, offset);
            if (id == NtlmRoutines.MsvAvEOL) break;
            int len = bytesTo2(messageData, offset+2);
            if (id <= NtlmRoutines.MsvAvDnsTreeName || id == NtlmRoutines.MsvAvTargetName) {
                String value = new String(messageData, offset + 4, len, UNICODE_ENCODING);
            } else if (id == NtlmRoutines.MsvAvTimestamp) {
//                time = bytesTo8(messageData, offset + 4);
//                timeOffset = offset + 4;
                time = new ByteArray(messageData, offset+4, 8);
            } if (id == NtlmRoutines.MsvAvFlags) {
                int msvAvFlagsValue = bytesTo4(messageData, offset + 4);
                msvAvFlag = NtlmRoutines.MsvAvFlag.values()[msvAvFlagsValue - 1];
            } else {

            }

            offset += len+4;
        }
//        targetInfo = new byte[targetInfoLen];
    }
}

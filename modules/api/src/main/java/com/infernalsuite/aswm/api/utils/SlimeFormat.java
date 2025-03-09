package com.infernalsuite.aswm.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class containing some standards of the SRF.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlimeFormat {

    /** First bytes of every SRF file **/
    public static final byte[] SLIME_HEADER = new byte[] { -79, 11 };

    /** Latest version of the SRF that SWM supports **/
    public static final byte SLIME_VERSION = 12;

}

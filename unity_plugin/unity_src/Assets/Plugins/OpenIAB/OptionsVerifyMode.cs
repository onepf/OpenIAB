using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace OnePF {
    public enum OptionsVerifyMode {
        /**
         * Verify signatures in any store. 
         * <p>
         * By default in Google's IabHelper. Throws exception if key is not available or invalid.
         * To prevent crashes OpenIAB wouldn't connect to OpenStore if no publicKey provided
         */
        VERIFY_EVERYTHING = 0,
        
        /**
         * Don't verify signatires. To perform verification on server-side
         */
        VERIFY_SKIP = 1,
        
        /**
         * Verify signatures only if publicKey is available. Otherwise skip verification. 
         * <p>
         * Developer is responsible for verify
         */
        VERIFY_ONLY_KNOWN = 2
    }
}

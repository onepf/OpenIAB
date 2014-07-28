/*******************************************************************************
 * Copyright 2012-2014 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace OnePF
{
    public enum OptionsVerifyMode
    {
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.marmotta.kiwi.sparql.function.rdfterm;

import org.apache.marmotta.kiwi.persistence.KiWiDialect;
import org.apache.marmotta.kiwi.persistence.mysql.MySQLDialect;
import org.apache.marmotta.kiwi.persistence.pgsql.PostgreSQLDialect;
import org.apache.marmotta.kiwi.sparql.builder.OPTypes;
import org.apache.marmotta.kiwi.sparql.function.NativeFunction;
import org.openrdf.query.algebra.evaluation.function.rdfterm.UUID;

/**
 * Add file description here!
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
public class NUUID extends UUID implements NativeFunction {

    /**
     * Return true if this function has available native support for the given dialect
     *
     * @param dialect
     * @return
     */
    @Override
    public boolean isSupported(KiWiDialect dialect) {
        return dialect instanceof PostgreSQLDialect || dialect instanceof MySQLDialect;
    }

    /**
     * Return a string representing how this function is translated into SQL in the given dialect
     *
     * @param dialect
     * @param args
     * @return
     */
    @Override
    public String getNative(KiWiDialect dialect, String... args) {
        if(dialect instanceof PostgreSQLDialect) {
            return "'urn:uuid:' || gen_random_uuid()";
        } else if(dialect instanceof MySQLDialect) {
            return "CONCAT('urn:uuid:', UUID())";
        }
        throw new UnsupportedOperationException("UUID not supported in dialect "+dialect);
    }

    /**
     * Get the return type of the function. This is needed for SQL type casting inside KiWi.
     *
     * @return
     */
    @Override
    public OPTypes getReturnType() {
        return OPTypes.URI;
    }

    /**
     * Get the argument type of the function for the arg'th argument (starting to count at 0).
     * This is needed for SQL type casting inside KiWi.
     *
     * @param arg
     * @return
     */
    @Override
    public OPTypes getArgumentType(int arg) {
        return OPTypes.ANY;
    }

    /**
     * Return the minimum number of arguments this function requires.
     *
     * @return
     */
    @Override
    public int getMinArgs() {
        return 0;
    }

    /**
     * Return the maximum number of arguments this function can take
     *
     * @return
     */
    @Override
    public int getMaxArgs() {
        return 0;
    }
}

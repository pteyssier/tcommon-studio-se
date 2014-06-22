// ============================================================================
//
// Copyright (C) 2006-2011 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.utils.string;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.talend.utils.sugars.ReturnCode;

/**
 * @author scorreia. A String utility class.
 */
public final class StringUtilities {

    private StringUtilities() {
    }

    /**
     * Method "tokenize".
     * 
     * @param input the string to split
     * @param delimiters the delimiter(s) of the fields
     * @see StringTokenizer
     * @return a list of Strings or an empty array when a null argument is given or when there is no string to split
     */
    public static List<String> tokenize(String input, String delimiters) {
        List<String> stringArray = new ArrayList<String>();

        if (input == null) {
            return stringArray;
        }
        if (delimiters == null) {
            return stringArray;
        }

        StringTokenizer t = new StringTokenizer(input, delimiters);

        while (t.hasMoreTokens()) {
            stringArray.add(t.nextToken());
        }
        return stringArray;
    } // eom tokenize

    /**
     * Method "checkBalancedParenthesis".
     * 
     * @param input the string to check
     * @param openingBlock can be a left parenthesis,...
     * @param closingBlock can be a right parenthesis...
     * @return true when the parenthesis are well balanced.
     */
    public static ReturnCode checkBalancedParenthesis(String input, char openingBlock, char closingBlock) {
        int level = 0;
        int i;
        for (i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == openingBlock) {
                level++;
            } else if (currentChar == closingBlock) {
                level--;
            }
            if (level < 0) {
                return new ReturnCode("too many " + closingBlock + " at position " + i, false);
            }
        }
        if (level < 0) {
            return new ReturnCode("too many " + closingBlock + " at position " + i, false);
        }
        if (level > 0) {
            return new ReturnCode("too many " + openingBlock + " at position " + i, false);
        }
        return new ReturnCode();
    }
}

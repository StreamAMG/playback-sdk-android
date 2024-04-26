/*!
 * This source file is part of the Swift.org open source project
 *
 * Copyright (c) 2021 Apple Inc. and the Swift project authors
 * Licensed under Apache License v2.0 with Runtime Library Exception
 *
 * See https://swift.org/LICENSE.txt for license information
 * See https://swift.org/CONTRIBUTORS.txt for Swift project authors
 */
(self["webpackChunkswift_docc_render"] = self["webpackChunkswift_docc_render"] || []).push([[788], {
    8257: function (e) {
        var n = "[0-9](_*[0-9])*",
            a = `\\.(${n})`,
            s = "[0-9a-fA-F](_*[0-9a-fA-F)*",
            t = {
                className: "number",
                variants: [{
                        begin: `(\\b(${n})((${a})|\\.)?|(${a}))[eE][+-]?(${n})[fFdD]?\\b`
                    },
                    {
                        begin: `\\b(${n})((${a})[fFdD]?\\b|\\.([fFdD]\\b)?)`
                    },
                    {
                        begin: `(${a})[fFdD]?\\b`
                    },
                    {
                        begin: `\\b(${n})[fFdD]\\b`
                    },
                    {
                        begin: `\\b0[xX]((${s})\\.?|(${s})?\\.(${s}))[pP][+-]?(${n})[fFdD]?\\b`
                    },
                    {
                        begin: "\\b(0|[1-9](_*[0-9])*)[lL]?\\b"
                    },
                    {
                        begin: `\\b0[xX](${s})[lL]?\\b`
                    },
                    {
                        begin: "\\b0(_*[0-7])*[lL]?\\b"
                    },
                    {
                        begin: "\\b0[bB][01](_*[01])*[lL]?\\b"
                    }
                ],
                relevance: 0
            };

        function r(e) {
            const n = "[À-ʸa-zA-Z_$][À-ʸa-zA-Z_$0-9]*",
                s = ["val", "var", "by", "get", "set", "lateinit", "data", "inline", "noinline", "tailrec", "external", "annotation", "crossinline", "const", "operator", "infix", "suspend", "actual", "expect", "sealed", "inner", "out", "enum", "fun", "companion", "object", "class", "interface", "typealias", "throw", "return", "else", "break", "continue", "if", "else", "for", "while", "do", "when", "try", "catch", "finally", "package", "import", "public", "private", "protected", "internal", "super", "this", "throw", "return", "true", "false", "null"],
                c = ["String", "Double", "Float", "Long", "Int", "Short", "Byte", "Boolean", "Char"],
                b = {
                    keyword: s,
                    literal: ["true", "false", "null"],
                    type: c,
                    built_in: ["super", "this"]
                },
                o = {
                    className: "meta",
                    begin: "@" + n,
                    contains: [{
                        begin: /\(/,
                        end: /\)/,
                        contains: ["self"]
                    }]
                },
                _ = {
                    className: "params",
                    begin: /\(/,
                    end: /\)/,
                    keywords: b,
                    relevance: 0,
                    contains: [e.C_BLOCK_COMMENT_MODE],
                    endsParent: true
                };

            return {
                name: "Kotlin",
                aliases: ["kt", "kts"],
                keywords: b,
                illegal: /<\/|#/,
                contains: [
                    e.COMMENT("/**", "*/", {
                        relevance: 0,
                        contains: [{
                            begin: /\w+@/,
                            relevance: 0
                        }, {
                            className: "doctag",
                            begin: "@[A-Za-z]+"
                        }]
                    }),
                    {
                        begin: /import\s+[^\n]+\./,
                        keywords: "import",
                        relevance: 2
                    },
                    e.C_LINE_COMMENT_MODE,
                    e.C_BLOCK_COMMENT_MODE,
                    {
                        begin: /"""/,
                        end: /"""/,
                        className: "string",
                        contains: [e.BACKSLASH_ESCAPE]
                    },
                    e.APOS_STRING_MODE,
                    e.QUOTE_STRING_MODE,
                    {
                        match: [/\b(?:class|interface|enum|extends|implements|new|typealias)/, /\s+/, n],
                        className: {
                            1: "keyword",
                            3: "title.class"
                        }
                    },
                    {
                        begin: [n, /\s+/, n, /\s+/, /=/],
                        className: {
                            1: "type",
                            3: "variable",
                            5: "operator"
                        }
                    },
                    {
                        begin: [/\b(?:fun)\b/, /\s+/, n],
                        className: {
                            1: "keyword",
                            3: "title.function"
                        },
                        contains: [_, e.C_LINE_COMMENT_MODE, e.C_BLOCK_COMMENT_MODE]
                    },
                    t, o
                ]
            };
        }

        e.exports = r
    }
}]);

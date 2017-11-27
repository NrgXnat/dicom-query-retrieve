/*
 * ---
 * list filtering
 * ---
 */

// create new case-insensitive :contains selector
// usage - $('.this_selector:containsNC("hello")').click(function() { ... });
$.extend($.expr[":"], {
    "containsNC": function(elem, i, match, array) {
        return (elem.textContent || elem.innerText || "")
            .toLowerCase()
            .indexOf((match[3] || "")
                .toLowerCase()) >= 0;
    }
});
// create new case-insensitive anti-':contains' selector
// usage - $('.this_selector:containsNC("hello")').click(function() { ... });
$.extend($.expr[":"], {
    "aintContainsNC": function(elem, i, match, array) {
        return (elem.textContent || elem.innerText || "")
            .toLowerCase()
            .indexOf((match[3] || "")
                .toLowerCase()) < 0;
    }
});


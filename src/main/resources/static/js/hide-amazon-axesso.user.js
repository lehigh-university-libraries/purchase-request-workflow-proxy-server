// ==UserScript==
// @name         Collapse Amazon Axesso Enrichment in Jira (Purchase Request Platform)
// @namespace    http://library.lehigh.edu
// @version      0.1
// @description  Auto-collapse the Jira comment with the Amazon Axesso pricing enrichment.
// @author       Maccabee Levine
// @match        https://jira.cc.lehigh.edu/*
// @match        https://jira-test.cc.lehigh.edu/*
// @grant        none
// ==/UserScript==

(function() {
    'use strict';

    const closeComment = (comment) => {
        if (comment.innerHTML.contains("Amazon")) {
            if (comment.className.includes("expanded")) {
                comment.querySelector('button[title^=Collapse]').click();
            }
        }
    };

    // Monitor for comments loaded after initial page load.
    let target = document.body;
    let config = { subtree: true, attributes: true, childList: true, characterData: true };
    const callback = (mutationList, observer) => {
        mutationList.forEach((mutation) => {
            mutation.addedNodes.forEach((addedNode) => {
                if (addedNode.nodeType == Node.ELEMENT_NODE) {
                    addedNode.querySelectorAll(".activity-comment").forEach((comment) => {
                        closeComment(comment);
                    });
                }
            });
        });
    };
    const observer = new MutationObserver(callback);
    observer.observe(target, config);

    // One time on page load.
    document.querySelectorAll(".activity-comment").forEach((comment) => {
        closeComment(comment);
    });


})();
package com.aaryo.velocity.util

object ReaderModeExtractor {

    const val EXTRACTOR_JS = """
        (function() {
            var article = document.querySelector('article') || document.querySelector('.article') || document.querySelector('main') || document.body;
            var title = (document.querySelector('h1') ? document.querySelector('h1').innerText : document.title) || '';
            var paragraphs = article.querySelectorAll('p');
            var textContent = [];
            for (var i = 0; i < paragraphs.length; i++) {
                var p = paragraphs[i].innerText.trim();
                if (p.length > 20) {
                    textContent.push('<p>' + p + '</p>');
                }
            }
            return JSON.stringify({
                title: title,
                content: textContent.join(''),
                url: window.location.href
            });
        })();
    """

    data class ArticleData(
        val title: String,
        val contentHtml: String,
        val url: String
    )
}

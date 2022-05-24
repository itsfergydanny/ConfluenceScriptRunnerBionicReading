def apiKey = BIONIC_KEY

def pageId = 753665 // Hardcoded for testing purposes but you can use "page.id" if running inside of a page related Script Listener
def getPageResponse = get("/wiki/rest/api/content/${pageId}")
        .queryString("expand", "body.storage.space")
        .asObject(Map)
if (getPageResponse.statusCode != 200) {
    return getPageResponse
}

// post to bionic api to translate our text
String pageBody = getPageResponse.body.body.storage.value // removeTags()
def bionicAPIResponse = post("https://bionic-reading1.p.rapidapi.com/convert")
        .header("content-type", "application/x-www-form-urlencoded")
        .header("X-RapidAPI-Host", "bionic-reading1.p.rapidapi.com")
        .header("X-RapidAPI-Key", apiKey)
        .body("content=" + URLEncoder.encode(pageBody, "UTF-8") + "&response_type=html&request_type=html&fixation=1&saccade=10")
        .asString()
if (bionicAPIResponse.statusCode != 200) {
    return bionicAPIResponse
}

// delete all previous comments (not ideal but fine for our test)
def fetchCommentsResponse = get("/wiki/rest/api/content/${pageId}/child/comment")
        .queryString("expand", "children.comment")
        .asObject(Map)
if (fetchCommentsResponse.statusCode != 200) {
    return fetchCommentsResponse
}
for (def comment : fetchCommentsResponse.body.results) {
    delete("/wiki/rest/api/content/${comment.id}")
        .asString()
}

// post a comment with the translated text
def bionicTitle = "Bionic Reading (see <a href=\"https://bionic-reading.com\">https://bionic-reading.com</a>) version of the page below:"
def createCommentResponse = post("/wiki/rest/api/content")
        .header("Content-Type", "application/json")
        .body(
                [
                        type: "comment",
                        body: [
                                storage: [
                                        representation: "storage",
                                        value         : "${bionicTitle}${bionicAPIResponse.body}"
                                ]
                        ],
                        container: [
                                id    : pageId,
                                type  : "page",
                                status: "current"
                        ]
                ]
        )
        .asString()
if (createCommentResponse.statusCode != 200) {
    return createCommentResponse
}

return "Bionic reading comment created for page: ${getPageResponse.body.title}"
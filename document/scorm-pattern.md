# SCORM Pattern - Sample Course Request/Response (Full)

Tài liệu này cung cấp **request** và **response** mẫu cho một course “thực tế”, bao gồm đầy đủ cấu trúc:

- Course
- Thumbnail
- Sections
- Pages (CONTENT/QUIZ)
- ContentPage + ContentBlocks
- QuizPage + danh sách Questions
- Payload question chi tiết bao gồm đầy đủ các dạng:
  - `MCQ_SINGLE`
  - `MCQ_MULTI`
  - `TRUE_FALSE`
  - `FILL_BLANK`
  - `MATCHING`
  - `SHORT_ANSWER`
  - `GROUPING`

Lưu ý:

- Các API hiện có:
  - Course detail trả nested structure theo `CourseDetailResponse`.
  - Quiz page trong course detail hiện chỉ trả `questions` dạng summary (`QuestionSummaryDto`).
  - Để “bao quát tất cả trường hợp”, phần dưới sẽ có thêm một block `questionsFull` mô phỏng việc FE gọi thêm `GET /questions/{id}` để lấy full details (đúng theo DTO `QuestionResponse`).

---

## A) Sample Request (Create full course data - conceptual)

Hiện tại backend đang tách nhiều API (create course, create section, create page, create content page/blocks, create quiz page, create question, attach question to quiz...). Do đó “request khổng lồ 1 lần” dưới đây mang tính **pattern** để bạn thấy full payload một course thực tế có thể được build từ nhiều bước.

```json
{
  "course": {
    "title": "Vietnamese Safety Training - SCORM 2004",
    "passingScore": 80.5,
    "attemptLimit": 3,
    "durationMin": 120,
    "status": "DRAFT",
    "extraInfor": {
      "category": "Compliance",
      "language": "vi",
      "level": "Beginner",
      "tags": ["safety", "onboarding", "scorm"],
      "version": "1.0.0",
      "estimatedReadingTimeMin": 90,
      "audience": {
        "roles": ["New hire", "Contractor"],
        "departments": ["Operations", "Warehouse"]
      }
    }
  },
  "thumbnail": {
    "imageMediaId": 123
  },
  "sections": [
    {
      "title": "Giới thiệu & mục tiêu",
      "description": "Tổng quan khóa học, mục tiêu học tập, cách tính điểm.",
      "orderIndex": 1,
      "learningObjective": "Nắm quy định cơ bản và hoàn thành bài kiểm tra đầu vào.",
      "themeOverride": {
        "tokens": {
          "primary": "#0F172A",
          "accent": "#22C55E",
          "fontFamily": "Inter"
        }
      },
      "pages": [
        {
          "title": "Chào mừng",
          "orderIndex": 1,
          "pageType": "CONTENT",
          "themeOverride": {
            "tokens": {
              "heroBg": "#F8FAFC",
              "headingSize": 28
            }
          },
          "contentPage": {
            "layoutType": "SINGLE_COLUMN",
            "blocks": [
              {
                "orderIndex": 1,
                "textHtml": "<h1>Chào mừng</h1><p>Bạn sẽ học các quy tắc an toàn cơ bản...</p>"
              },
              {
                "orderIndex": 2,
                "textHtml": "<p><strong>Lưu ý:</strong> Hoàn thành tất cả bài học trước khi làm bài kiểm tra.</p>"
              }
            ]
          }
        },
        {
          "title": "Quiz đầu vào",
          "orderIndex": 2,
          "pageType": "QUIZ",
          "themeOverride": null,
          "quizPage": {
            "passingScore": 60.0,
            "attemptAllowed": 2,
            "questions": [
              {
                "question": {
                  "title": "MCQ Single - PPE bắt buộc",
                  "instruction": "Chọn 1 đáp án đúng.",
                  "promptHtml": "<p>Khi vào khu vực kho, PPE nào là bắt buộc?</p>",
                  "questionType": "MCQ_SINGLE",
                  "points": 1,
                  "shuffleOptions": true,
                  "caseSensitive": false,
                  "extraConfig": {
                    "feedback": {
                      "correct": "Chính xác!",
                      "incorrect": "Hãy đọc lại quy định PPE."
                    }
                  }
                },
                "details": {
                  "options": [
                    {
                      "orderIndex": 1,
                      "contentHtml": "<p>Mũ bảo hiểm</p>",
                      "isCorrect": true,
                      "scoreFraction": 1.0
                    },
                    {
                      "orderIndex": 2,
                      "contentHtml": "<p>Dép lê</p>",
                      "isCorrect": false,
                      "scoreFraction": 0.0
                    },
                    {
                      "orderIndex": 3,
                      "contentHtml": "<p>Không cần gì</p>",
                      "isCorrect": false,
                      "scoreFraction": 0.0
                    }
                  ]
                }
              },
              {
                "question": {
                  "title": "True/False - Báo cáo sự cố",
                  "instruction": "Chọn Đúng hoặc Sai.",
                  "promptHtml": "<p>Chỉ cần báo cáo sự cố khi có người bị thương.</p>",
                  "questionType": "TRUE_FALSE",
                  "points": 1,
                  "shuffleOptions": false,
                  "caseSensitive": false,
                  "extraConfig": {
                    "hint": "Sự cố suýt xảy ra (near-miss) cũng cần báo cáo."
                  }
                },
                "details": {
                  "correctValue": false
                }
              }
            ]
          }
        }
      ]
    },
    {
      "title": "An toàn vận hành",
      "description": "Quy trình làm việc an toàn và nhận diện rủi ro.",
      "orderIndex": 2,
      "learningObjective": "Nhận diện hazard, áp dụng SOP đúng.",
      "themeOverride": null,
      "pages": [
        {
          "title": "Nhận diện nguy cơ",
          "orderIndex": 1,
          "pageType": "CONTENT",
          "themeOverride": {
            "tokens": {
              "calloutBg": "#FEF3C7"
            }
          },
          "contentPage": {
            "layoutType": "TWO_COLUMN",
            "blocks": [
              {
                "orderIndex": 1,
                "textHtml": "<h2>Hazard là gì?</h2><p>Hazard là nguồn gây hại tiềm ẩn...</p>"
              },
              {
                "orderIndex": 2,
                "textHtml": "<ul><li>Trượt ngã</li><li>Vật rơi</li><li>Điện giật</li></ul>"
              },
              {
                "orderIndex": 3,
                "textHtml": "<p><em>Ví dụ thực tế:</em> Sàn ướt ở khu vực đóng gói...</p>"
              }
            ]
          }
        },
        {
          "title": "Quiz - An toàn vận hành (tổng hợp)",
          "orderIndex": 2,
          "pageType": "QUIZ",
          "themeOverride": {
            "tokens": { "primary": "#7C3AED" }
          },
          "quizPage": {
            "passingScore": 80.0,
            "attemptAllowed": 3,
            "questions": [
              {
                "question": {
                  "title": "MCQ Multi - Chọn các hành vi an toàn",
                  "instruction": "Có thể có nhiều đáp án đúng.",
                  "promptHtml": "<p>Những hành vi nào sau đây là an toàn?</p>",
                  "questionType": "MCQ_MULTI",
                  "points": 2,
                  "shuffleOptions": true,
                  "caseSensitive": false,
                  "extraConfig": {
                    "partialCredit": true,
                    "scoring": "FRACTION"
                  }
                },
                "details": {
                  "options": [
                    {
                      "orderIndex": 1,
                      "contentHtml": "<p>Đặt biển cảnh báo khi sàn ướt</p>",
                      "isCorrect": true,
                      "scoreFraction": 0.5
                    },
                    {
                      "orderIndex": 2,
                      "contentHtml": "<p>Chạy trong khu vực kho</p>",
                      "isCorrect": false,
                      "scoreFraction": 0.0
                    },
                    {
                      "orderIndex": 3,
                      "contentHtml": "<p>Kiểm tra xe nâng trước khi dùng</p>",
                      "isCorrect": true,
                      "scoreFraction": 0.5
                    },
                    {
                      "orderIndex": 4,
                      "contentHtml": "<p>Tắt thiết bị bảo hộ để làm nhanh hơn</p>",
                      "isCorrect": false,
                      "scoreFraction": 0.0
                    }
                  ]
                }
              },
              {
                "question": {
                  "title": "Fill Blank - Quy trình khóa an toàn (LOTO)",
                  "instruction": "Điền vào chỗ trống.",
                  "promptHtml": "<p>Quy trình <strong>LOTO</strong> gồm: <span data-blank=\"b1\"></span>, <span data-blank=\"b2\"></span>, và <span data-blank=\"b3\"></span>.</p>",
                  "questionType": "FILL_BLANK",
                  "points": 3,
                  "shuffleOptions": false,
                  "caseSensitive": false,
                  "extraConfig": {
                    "normalize": "TRIM_LOWER"
                  }
                },
                "details": {
                  "orderedBlanks": true,
                  "blanks": [
                    {
                      "blankKey": "b1",
                      "orderIndex": 1,
                      "answers": [
                        {
                          "answerText": "Cô lập năng lượng",
                          "matchRule": "EXACT"
                        },
                        {
                          "answerText": "co lap nang luong",
                          "matchRule": "CASE_INSENSITIVE"
                        }
                      ]
                    },
                    {
                      "blankKey": "b2",
                      "orderIndex": 2,
                      "answers": [
                        {
                          "answerText": "Khóa và gắn thẻ",
                          "matchRule": "EXACT"
                        },
                        {
                          "answerText": "khoa va gan the",
                          "matchRule": "CASE_INSENSITIVE"
                        }
                      ]
                    },
                    {
                      "blankKey": "b3",
                      "orderIndex": 3,
                      "answers": [
                        {
                          "answerText": "Xác nhận cô lập",
                          "matchRule": "EXACT"
                        },
                        {
                          "answerText": "xac nhan co lap",
                          "matchRule": "CASE_INSENSITIVE"
                        }
                      ]
                    }
                  ]
                }
              },
              {
                "question": {
                  "title": "Matching - Ghép thuật ngữ",
                  "instruction": "Kéo thả để ghép đúng.",
                  "promptHtml": "<p>Ghép thuật ngữ (trái) với định nghĩa (phải).</p>",
                  "questionType": "MATCHING",
                  "points": 3,
                  "shuffleOptions": false,
                  "caseSensitive": false,
                  "extraConfig": null
                },
                "details": {
                  "leftItems": [
                    { "orderIndex": 1, "contentHtml": "<p>Hazard</p>" },
                    { "orderIndex": 2, "contentHtml": "<p>Risk</p>" },
                    { "orderIndex": 3, "contentHtml": "<p>Control</p>" }
                  ],
                  "rightItems": [
                    {
                      "orderIndex": 1,
                      "contentHtml": "<p>Khả năng xảy ra + mức độ hậu quả</p>"
                    },
                    {
                      "orderIndex": 2,
                      "contentHtml": "<p>Biện pháp giảm thiểu</p>"
                    },
                    { "orderIndex": 3, "contentHtml": "<p>Nguồn gây hại</p>" }
                  ],
                  "pairs": [
                    { "leftOrderIndex": 1, "rightOrderIndex": 3, "score": 1 },
                    { "leftOrderIndex": 2, "rightOrderIndex": 1, "score": 1 },
                    { "leftOrderIndex": 3, "rightOrderIndex": 2, "score": 1 }
                  ]
                }
              },
              {
                "question": {
                  "title": "Short Answer - Mô tả near-miss",
                  "instruction": "Trả lời ngắn.",
                  "promptHtml": "<p>Near-miss là gì? Hãy mô tả ngắn gọn.</p>",
                  "questionType": "SHORT_ANSWER",
                  "points": 2,
                  "shuffleOptions": false,
                  "caseSensitive": false,
                  "extraConfig": {
                    "grading": "KEYWORD",
                    "keywords": ["suýt", "xảy ra", "không gây thương tích"]
                  }
                },
                "details": {
                  "minLength": 20,
                  "maxLength": 200,
                  "expectedAnswers": [
                    {
                      "answerText": "Sự cố suýt xảy ra nhưng không gây hậu quả",
                      "matchRule": "CONTAINS"
                    },
                    {
                      "answerText": "Near-miss là tình huống có thể gây tai nạn nhưng chưa xảy ra",
                      "matchRule": "CONTAINS"
                    }
                  ]
                }
              },
              {
                "question": {
                  "title": "Grouping - Phân loại hành vi",
                  "instruction": "Kéo các mục vào nhóm phù hợp.",
                  "promptHtml": "<p>Phân loại hành vi theo nhóm <strong>An toàn</strong> và <strong>Không an toàn</strong>.</p>",
                  "questionType": "GROUPING",
                  "points": 4,
                  "shuffleOptions": false,
                  "caseSensitive": false,
                  "extraConfig": { "ui": "DRAG_DROP" }
                },
                "details": {
                  "groups": [
                    {
                      "orderIndex": 1,
                      "title": "An toàn",
                      "description": "Hành vi đúng quy trình"
                    },
                    {
                      "orderIndex": 2,
                      "title": "Không an toàn",
                      "description": "Hành vi vi phạm"
                    }
                  ],
                  "items": [
                    {
                      "orderIndex": 1,
                      "contentHtml": "<p>Đeo kính bảo hộ khi cắt kim loại</p>"
                    },
                    {
                      "orderIndex": 2,
                      "contentHtml": "<p>Bỏ qua khóa an toàn để sửa nhanh</p>"
                    },
                    {
                      "orderIndex": 3,
                      "contentHtml": "<p>Báo cáo near-miss ngay lập tức</p>"
                    },
                    {
                      "orderIndex": 4,
                      "contentHtml": "<p>Đặt tay gần khu vực kẹp của máy</p>"
                    }
                  ],
                  "itemAnswers": [
                    { "itemOrderIndex": 1, "groupOrderIndex": 1 },
                    { "itemOrderIndex": 2, "groupOrderIndex": 2 },
                    { "itemOrderIndex": 3, "groupOrderIndex": 1 },
                    { "itemOrderIndex": 4, "groupOrderIndex": 2 }
                  ]
                }
              }
            ]
          }
        }
      ]
    }
  ]
}
```

---

## B) Sample Response (GET `/courses/{courseId}` - nested)

Dưới đây mô phỏng response theo `CourseDetailResponse` (course -> sections -> pages -> contentPage/quizPage). Lưu ý quiz page chỉ có `questions` dạng summary.

```json
{
  "courseId": 101,
  "title": "Vietnamese Safety Training - SCORM 2004",
  "passingScore": 80.5,
  "attemptLimit": 3,
  "durationMin": 120,
  "status": "DRAFT",
  "lastPublishedAt": null,
  "updatedAt": "2026-01-21T06:30:00Z",
  "extraInfor": {
    "category": "Compliance",
    "language": "vi",
    "level": "Beginner",
    "tags": ["safety", "onboarding", "scorm"],
    "version": "1.0.0",
    "estimatedReadingTimeMin": 90,
    "audience": {
      "roles": ["New hire", "Contractor"],
      "departments": ["Operations", "Warehouse"]
    }
  },
  "createdAt": "2026-01-20T10:00:00Z",
  "courseUserId": 5,
  "thumbnail": {
    "courseId": 101,
    "imageMediaId": 123
  },
  "sections": [
    {
      "sectionId": 201,
      "title": "Giới thiệu & mục tiêu",
      "description": "Tổng quan khóa học, mục tiêu học tập, cách tính điểm.",
      "orderIndex": 1,
      "learningObjective": "Nắm quy định cơ bản và hoàn thành bài kiểm tra đầu vào.",
      "themeOverride": {
        "tokens": {
          "primary": "#0F172A",
          "accent": "#22C55E",
          "fontFamily": "Inter"
        }
      },
      "pages": [
        {
          "pageId": 301,
          "title": "Chào mừng",
          "orderIndex": 1,
          "pageType": "CONTENT",
          "themeOverride": {
            "tokens": { "heroBg": "#F8FAFC", "headingSize": 28 }
          },
          "contentPage": {
            "pageId": 301,
            "layoutType": "SINGLE_COLUMN",
            "blocks": [
              {
                "blockId": 401,
                "orderIndex": 1,
                "textHtml": "<h1>Chào mừng</h1><p>Bạn sẽ học các quy tắc an toàn cơ bản...</p>",
                "contentPageId": 301
              },
              {
                "blockId": 402,
                "orderIndex": 2,
                "textHtml": "<p><strong>Lưu ý:</strong> Hoàn thành tất cả bài học trước khi làm bài kiểm tra.</p>",
                "contentPageId": 301
              }
            ]
          },
          "quizPage": null
        },
        {
          "pageId": 302,
          "title": "Quiz đầu vào",
          "orderIndex": 2,
          "pageType": "QUIZ",
          "themeOverride": null,
          "contentPage": null,
          "quizPage": {
            "pageId": 302,
            "passingScore": 60.0,
            "attemptAllowed": 2,
            "questions": [
              {
                "questionId": 1001,
                "title": "MCQ Single - PPE bắt buộc",
                "questionType": "MCQ_SINGLE"
              },
              {
                "questionId": 1002,
                "title": "True/False - Báo cáo sự cố",
                "questionType": "TRUE_FALSE"
              }
            ]
          }
        }
      ]
    },
    {
      "sectionId": 202,
      "title": "An toàn vận hành",
      "description": "Quy trình làm việc an toàn và nhận diện rủi ro.",
      "orderIndex": 2,
      "learningObjective": "Nhận diện hazard, áp dụng SOP đúng.",
      "themeOverride": null,
      "pages": [
        {
          "pageId": 303,
          "title": "Nhận diện nguy cơ",
          "orderIndex": 1,
          "pageType": "CONTENT",
          "themeOverride": { "tokens": { "calloutBg": "#FEF3C7" } },
          "contentPage": {
            "pageId": 303,
            "layoutType": "TWO_COLUMN",
            "blocks": [
              {
                "blockId": 403,
                "orderIndex": 1,
                "textHtml": "<h2>Hazard là gì?</h2><p>Hazard là nguồn gây hại tiềm ẩn...</p>",
                "contentPageId": 303
              },
              {
                "blockId": 404,
                "orderIndex": 2,
                "textHtml": "<ul><li>Trượt ngã</li><li>Vật rơi</li><li>Điện giật</li></ul>",
                "contentPageId": 303
              },
              {
                "blockId": 405,
                "orderIndex": 3,
                "textHtml": "<p><em>Ví dụ thực tế:</em> Sàn ướt ở khu vực đóng gói...</p>",
                "contentPageId": 303
              }
            ]
          },
          "quizPage": null
        },
        {
          "pageId": 304,
          "title": "Quiz - An toàn vận hành (tổng hợp)",
          "orderIndex": 2,
          "pageType": "QUIZ",
          "themeOverride": { "tokens": { "primary": "#7C3AED" } },
          "contentPage": null,
          "quizPage": {
            "pageId": 304,
            "passingScore": 80.0,
            "attemptAllowed": 3,
            "questions": [
              {
                "questionId": 1003,
                "title": "MCQ Multi - Chọn các hành vi an toàn",
                "questionType": "MCQ_MULTI"
              },
              {
                "questionId": 1004,
                "title": "Fill Blank - Quy trình khóa an toàn (LOTO)",
                "questionType": "FILL_BLANK"
              },
              {
                "questionId": 1005,
                "title": "Matching - Ghép thuật ngữ",
                "questionType": "MATCHING"
              },
              {
                "questionId": 1006,
                "title": "Short Answer - Mô tả near-miss",
                "questionType": "SHORT_ANSWER"
              },
              {
                "questionId": 1007,
                "title": "Grouping - Phân loại hành vi",
                "questionType": "GROUPING"
              }
            ]
          }
        }
      ]
    }
  ],

  "questionsFull": [
    {
      "question": {
        "questionId": 1001,
        "title": "MCQ Single - PPE bắt buộc",
        "instruction": "Chọn 1 đáp án đúng.",
        "promptHtml": "<p>Khi vào khu vực kho, PPE nào là bắt buộc?</p>",
        "questionType": "MCQ_SINGLE",
        "points": 1,
        "shuffleOptions": true,
        "caseSensitive": false,
        "extraConfig": {
          "feedback": {
            "correct": "Chính xác!",
            "incorrect": "Hãy đọc lại quy định PPE."
          }
        }
      },
      "details": {
        "options": [
          {
            "optionId": 5001,
            "orderIndex": 1,
            "contentHtml": "<p>Mũ bảo hiểm</p>",
            "isCorrect": true,
            "scoreFraction": 1.0
          },
          {
            "optionId": 5002,
            "orderIndex": 2,
            "contentHtml": "<p>Dép lê</p>",
            "isCorrect": false,
            "scoreFraction": 0.0
          },
          {
            "optionId": 5003,
            "orderIndex": 3,
            "contentHtml": "<p>Không cần gì</p>",
            "isCorrect": false,
            "scoreFraction": 0.0
          }
        ]
      }
    },
    {
      "question": {
        "questionId": 1002,
        "title": "True/False - Báo cáo sự cố",
        "instruction": "Chọn Đúng hoặc Sai.",
        "promptHtml": "<p>Chỉ cần báo cáo sự cố khi có người bị thương.</p>",
        "questionType": "TRUE_FALSE",
        "points": 1,
        "shuffleOptions": false,
        "caseSensitive": false,
        "extraConfig": { "hint": "Near-miss cũng cần báo cáo." }
      },
      "details": { "correctValue": false }
    },
    {
      "question": {
        "questionId": 1003,
        "title": "MCQ Multi - Chọn các hành vi an toàn",
        "instruction": "Có thể có nhiều đáp án đúng.",
        "promptHtml": "<p>Những hành vi nào sau đây là an toàn?</p>",
        "questionType": "MCQ_MULTI",
        "points": 2,
        "shuffleOptions": true,
        "caseSensitive": false,
        "extraConfig": { "partialCredit": true, "scoring": "FRACTION" }
      },
      "details": {
        "options": [
          {
            "optionId": 5004,
            "orderIndex": 1,
            "contentHtml": "<p>Đặt biển cảnh báo khi sàn ướt</p>",
            "isCorrect": true,
            "scoreFraction": 0.5
          },
          {
            "optionId": 5005,
            "orderIndex": 2,
            "contentHtml": "<p>Chạy trong khu vực kho</p>",
            "isCorrect": false,
            "scoreFraction": 0.0
          },
          {
            "optionId": 5006,
            "orderIndex": 3,
            "contentHtml": "<p>Kiểm tra xe nâng trước khi dùng</p>",
            "isCorrect": true,
            "scoreFraction": 0.5
          },
          {
            "optionId": 5007,
            "orderIndex": 4,
            "contentHtml": "<p>Tắt thiết bị bảo hộ để làm nhanh hơn</p>",
            "isCorrect": false,
            "scoreFraction": 0.0
          }
        ]
      }
    },
    {
      "question": {
        "questionId": 1004,
        "title": "Fill Blank - Quy trình khóa an toàn (LOTO)",
        "instruction": "Điền vào chỗ trống.",
        "promptHtml": "<p>Quy trình <strong>LOTO</strong> gồm: <span data-blank=\"b1\"></span>, <span data-blank=\"b2\"></span>, và <span data-blank=\"b3\"></span>.</p>",
        "questionType": "FILL_BLANK",
        "points": 3,
        "shuffleOptions": false,
        "caseSensitive": false,
        "extraConfig": { "normalize": "TRIM_LOWER" }
      },
      "details": {
        "orderedBlanks": true,
        "blanks": [
          {
            "blankId": 6001,
            "blankKey": "b1",
            "orderIndex": 1,
            "answers": [
              {
                "answerId": 7001,
                "answerText": "Cô lập năng lượng",
                "matchRule": "EXACT"
              },
              {
                "answerId": 7002,
                "answerText": "co lap nang luong",
                "matchRule": "CASE_INSENSITIVE"
              }
            ]
          },
          {
            "blankId": 6002,
            "blankKey": "b2",
            "orderIndex": 2,
            "answers": [
              {
                "answerId": 7003,
                "answerText": "Khóa và gắn thẻ",
                "matchRule": "EXACT"
              },
              {
                "answerId": 7004,
                "answerText": "khoa va gan the",
                "matchRule": "CASE_INSENSITIVE"
              }
            ]
          },
          {
            "blankId": 6003,
            "blankKey": "b3",
            "orderIndex": 3,
            "answers": [
              {
                "answerId": 7005,
                "answerText": "Xác nhận cô lập",
                "matchRule": "EXACT"
              },
              {
                "answerId": 7006,
                "answerText": "xac nhan co lap",
                "matchRule": "CASE_INSENSITIVE"
              }
            ]
          }
        ]
      }
    },
    {
      "question": {
        "questionId": 1005,
        "title": "Matching - Ghép thuật ngữ",
        "instruction": "Kéo thả để ghép đúng.",
        "promptHtml": "<p>Ghép thuật ngữ (trái) với định nghĩa (phải).</p>",
        "questionType": "MATCHING",
        "points": 3,
        "shuffleOptions": false,
        "caseSensitive": false,
        "extraConfig": null
      },
      "details": {
        "leftItems": [
          { "leftId": 8001, "orderIndex": 1, "contentHtml": "<p>Hazard</p>" },
          { "leftId": 8002, "orderIndex": 2, "contentHtml": "<p>Risk</p>" },
          { "leftId": 8003, "orderIndex": 3, "contentHtml": "<p>Control</p>" }
        ],
        "rightItems": [
          {
            "rightId": 9001,
            "orderIndex": 1,
            "contentHtml": "<p>Khả năng xảy ra + mức độ hậu quả</p>"
          },
          {
            "rightId": 9002,
            "orderIndex": 2,
            "contentHtml": "<p>Biện pháp giảm thiểu</p>"
          },
          {
            "rightId": 9003,
            "orderIndex": 3,
            "contentHtml": "<p>Nguồn gây hại</p>"
          }
        ],
        "pairs": [
          { "pairId": 9101, "leftId": 8001, "rightId": 9003, "score": 1 },
          { "pairId": 9102, "leftId": 8002, "rightId": 9001, "score": 1 },
          { "pairId": 9103, "leftId": 8003, "rightId": 9002, "score": 1 }
        ]
      }
    },
    {
      "question": {
        "questionId": 1006,
        "title": "Short Answer - Mô tả near-miss",
        "instruction": "Trả lời ngắn.",
        "promptHtml": "<p>Near-miss là gì? Hãy mô tả ngắn gọn.</p>",
        "questionType": "SHORT_ANSWER",
        "points": 2,
        "shuffleOptions": false,
        "caseSensitive": false,
        "extraConfig": {
          "grading": "KEYWORD",
          "keywords": ["suýt", "xảy ra", "không gây thương tích"]
        }
      },
      "details": {
        "minLength": 20,
        "maxLength": 200,
        "expectedAnswers": [
          {
            "expectedId": 9201,
            "answerText": "Sự cố suýt xảy ra nhưng không gây hậu quả",
            "matchRule": "CONTAINS"
          },
          {
            "expectedId": 9202,
            "answerText": "Tình huống có thể gây tai nạn nhưng chưa xảy ra",
            "matchRule": "CONTAINS"
          }
        ]
      }
    },
    {
      "question": {
        "questionId": 1007,
        "title": "Grouping - Phân loại hành vi",
        "instruction": "Kéo các mục vào nhóm phù hợp.",
        "promptHtml": "<p>Phân loại hành vi theo nhóm <strong>An toàn</strong> và <strong>Không an toàn</strong>.</p>",
        "questionType": "GROUPING",
        "points": 4,
        "shuffleOptions": false,
        "caseSensitive": false,
        "extraConfig": { "ui": "DRAG_DROP" }
      },
      "details": {
        "groups": [
          {
            "groupId": 9301,
            "orderIndex": 1,
            "title": "An toàn",
            "description": "Hành vi đúng quy trình"
          },
          {
            "groupId": 9302,
            "orderIndex": 2,
            "title": "Không an toàn",
            "description": "Hành vi vi phạm"
          }
        ],
        "items": [
          {
            "itemId": 9401,
            "orderIndex": 1,
            "contentHtml": "<p>Đeo kính bảo hộ khi cắt kim loại</p>"
          },
          {
            "itemId": 9402,
            "orderIndex": 2,
            "contentHtml": "<p>Bỏ qua khóa an toàn để sửa nhanh</p>"
          },
          {
            "itemId": 9403,
            "orderIndex": 3,
            "contentHtml": "<p>Báo cáo near-miss ngay lập tức</p>"
          },
          {
            "itemId": 9404,
            "orderIndex": 4,
            "contentHtml": "<p>Đặt tay gần khu vực kẹp của máy</p>"
          }
        ],
        "itemAnswers": [
          { "answerId": 9501, "itemId": 9401, "groupId": 9301 },
          { "answerId": 9502, "itemId": 9402, "groupId": 9302 },
          { "answerId": 9503, "itemId": 9403, "groupId": 9301 },
          { "answerId": 9504, "itemId": 9404, "groupId": 9302 }
        ]
      }
    }
  ]
}
```

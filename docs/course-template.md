{
    "courseId": 82,
    "title": "Nhập môn Python",
    "description": "Mô tả: Khóa học tinh gọn nhất dành cho người mới bắt đầu, tập trung vào cú pháp cốt lõi để bạn có thể viết chương trình đầu tiên ngay lập tức.",
    "coverImageUrl": "https://pub-d9efd09f55ef46ada4a3be608c38264b.r2.dev/images/dcbb5554-5ed5-4657-b1e0-51902eebd7ba-python-co-ban_b80bca9b238b4615b94541de28af00ae.png",
    "passingScore": 0.00,
    "attemptLimit": 0,
    "durationMin": 0,
    "status": "Draft",
    "tags": null,
    "isFavorite": false,
    "textHtml": null,
    "themeOverride": {
        "global": {
            "textColor": "#111827",
            "background": "#ffffff",
            "fontFamily": "Inter, sans-serif",
            "borderRadius": 12,
            "primaryColor": "#2563eb"
        },
        "pageOverrides": {},
        "blockOverrides": {},
        "sectionOverrides": {}
    },
    "layoutMode": null,
    "layoutMeta": null,
    "lastPublishedAt": null,
    "updatedAt": "2026-04-13T15:00:07.814512Z",
    "extraInfor": {},
    "editorState": {
        "title": "Nhập môn Python",
        "sections": [
            {
                "id": "dfbf9946-963a-4acb-948a-104d3b9f17d3",
                "pages": [
                    {
                        "id": "6b02461d-d241-496d-81b7-b451bf2d2a51",
                        "title": "1. Biến và Lệnh Xuất (Print)",
                        "pageType": "CONTENT",
                        "contentPage": {
                            "blocks": [
                                {
                                    "id": "42543681-f549-4675-b35f-30f7ee0abae6",
                                    "type": "TEXT",
                                    "textHtml": "<p><response-element class=\"\" ng-version=\"0.0.0-PLACEHOLDER\"><!----><!----><!----><!----><!----><!----><code-block _nghost-ng-c3564457479=\"\" class=\"ng-tns-c3564457479-26 ng-star-inserted\" style=\"\"><!----><!----><div _ngcontent-ng-c3564457479=\"\" class=\"code-block ng-tns-c3564457479-26 ng-animate-disabled ng-trigger ng-trigger-codeBlockRevealAnimation\" jslog=\"223238;track:impression,attention;BardVeMetadataKey:[[&quot;r_283c3776a890fcd0&quot;,&quot;c_76adf253fa0f86b4&quot;,null,&quot;rc_7b529e2e8cb0cc20&quot;,null,null,&quot;vi&quot;,null,1,null,null,1,1]]\" data-hveid=\"0\" decode-data-ved=\"1\" data-ved=\"0CAAQhtANahcKEwjiz7q4iuuTAxUAAAAAHQAAAAAQHQ\" style=\"display: block;\"><div _ngcontent-ng-c3564457479=\"\" class=\"code-block-decoration header-formatted gds-title-s ng-tns-c3564457479-26 ng-star-inserted\" style=\"\"><span _ngcontent-ng-c3564457479=\"\" class=\"ng-tns-c3564457479-26\"></span></div></div></code-block></response-element></p><ul data-path-to-node=\"6\"><li><p data-path-to-node=\"6,0,0\"><b data-path-to-node=\"6,0,0\" data-index-in-node=\"0\">Biến:</b> Dùng để lưu trữ thông tin.</p></li><li><p data-path-to-node=\"6,1,0\"><b data-path-to-node=\"6,1,0\" data-index-in-node=\"0\">print():</b> Hàm dùng để hiển thị dữ liệu ra màn hình.</p></li></ul>",
                                    "orderIndex": 1
                                },
                                {
                                    "id": "539c8fe1-2ec4-4ed2-af58-317959a4ec69",
                                    "type": "TEXT",
                                    "textHtml": "<h2>Bài 1: Biến và Kiểu Dữ Liệu Cơ Bản</h2><p>Chào mừng bạn đến với bài học đầu tiên trong hành trình chinh phục Python! Hãy tưởng tượng bạn đang dọn dẹp nhà cửa và có rất nhiều đồ vật. Để dễ dàng tìm kiếm sau này, bạn cho chúng vào những chiếc hộp và dán nhãn lên đó. Trong lập trình, những \"chiếc hộp có nhãn\" đó được gọi là <strong>biến</strong>.</p><p>Trong bài học này, chúng ta sẽ cùng nhau tìm hiểu cách tạo ra những \"chiếc hộp\" này và những loại \"đồ vật\" (dữ liệu) cơ bản nhất có thể bỏ vào đó.</p><h3>1. Biến (Variable) là gì?</h3><p>Biến là một cái tên mà chúng ta đặt ra để lưu trữ một giá trị dữ liệu. Thay vì phải nhớ giá trị cụ thể (như số <code>1990</code>), chúng ta có thể gán nó cho một biến có tên dễ nhớ (như <code>nam_sinh</code>).</p><p>Trong Python, chúng ta sử dụng dấu bằng <code>=</code> để gán giá trị cho một biến. Cú pháp rất đơn giản:</p><em>tên_biến = giá_trị</em><blockquote><p># Ví dụ về gán biến<br>ten_sach = \"Lập trình Python\"<br>so_trang = 300<br>gia_bia = 150.5</p></blockquote><p>Ở ví dụ trên, chúng ta đã tạo ra 3 biến: <code>ten_sach</code> lưu trữ một đoạn văn bản, <code>so_trang</code> lưu một số nguyên, và <code>gia_bia</code> lưu một số thập phân.</p><h3>2. Các Kiểu Dữ Liệu Cơ Bản</h3><p>Python cần biết loại dữ liệu mà biến đang lưu trữ là gì để xử lý cho đúng. Dưới đây là những kiểu dữ liệu cơ bản và phổ biến nhất bạn sẽ gặp:</p><ul><li><strong>Số (Numbers):</strong> Dùng để lưu trữ các giá trị số.<ul><li><strong>Số nguyên (Integer):</strong> Là các số nguyên, không có phần thập phân. Ví dụ: <code>10</code>, <code>-5</code>, <code>1000</code>.<br><em>Ví dụ:</em> <code>so_luong_hoc_vien = 50</code></li><li><strong>Số thực (Float):</strong> Là các số có phần thập phân. Ví dụ: <code>3.14</code>, <code>-0.5</code>, <code>9.99</code>.<br><em>Ví dụ:</em> <code>diem_trung_binh = 8.5</code></li></ul></li><li><strong>Chuỗi (String):</strong> Dùng để lưu trữ văn bản. Giá trị chuỗi phải được đặt trong dấu nháy đơn <code>' '</code> hoặc nháy kép <code>\" \"</code>.<br><em>Ví dụ:</em> <code>loi_chao = \"Xin chào các bạn!\"</code></li><li><strong>Boolean:</strong> Là kiểu dữ liệu đặc biệt chỉ có hai giá trị: <code>True</code> (Đúng) hoặc <code>False</code> (Sai). Nó thường được dùng để biểu thị một điều kiện nào đó.<br><em>Ví dụ:</em> <code>da_hoan_thanh_bai_tap = True</code></li></ul><h3>3. Quy tắc đặt tên biến</h3><p>Để chương trình dễ đọc và tránh lỗi, bạn cần tuân theo một vài quy tắc khi đặt tên biến:</p><ul><li>Tên biến phải bắt đầu bằng một chữ cái hoặc dấu gạch dưới <code>_</code>.</li><li>Tên biến không được bắt đầu bằng số.</li><li>Tên biến chỉ có thể chứa các ký tự chữ-số (A-z, 0-9) và dấu gạch dưới <code>_</code>.</li><li>Tên biến có phân biệt chữ hoa, chữ thường (<code>tuoi</code>, <code>Tuoi</code> và <code>TUOI</code> là ba biến khác nhau).</li></ul><blockquote><p><strong>Mẹo hay:</strong> Hãy đặt tên biến thật gợi nhớ và dễ hiểu. Trong Python, cộng đồng thường dùng kiểu <em>snake_case</em> (các từ nối với nhau bằng dấu gạch dưới) để đặt tên biến, ví dụ: <code>ten_khach_hang</code>, <code>so_dien_thoai</code>.</p></blockquote><h3>Tóm tắt</h3><p>Vậy là chúng ta đã đi qua những khái niệm nền tảng nhất. Hãy cùng điểm lại nhé:</p><ul><li><strong>Biến</strong> giống như một chiếc hộp được dán nhãn để lưu trữ dữ liệu.</li><li>Sử dụng dấu <code>=</code> để gán giá trị cho một biến.</li><li>Các kiểu dữ liệu cơ bản gồm có: <strong>Số</strong> (nguyên và thực), <strong>Chuỗi</strong> (văn bản), và <strong>Boolean</strong> (True/False).</li><li>Việc đặt tên biến cần tuân thủ quy tắc và nên rõ ràng, dễ hiểu.</li></ul><p>Chúc mừng bạn đã hoàn thành bài học đầu tiên! Bây giờ, hãy thử tự mình tạo ra vài biến với các kiểu dữ liệu khác nhau nhé.</p>",
                                    "orderIndex": 2
                                },
                                {
                                    "id": "2b65dbc4-c78b-4f69-940b-8501d977bab1",
                                    "type": "VIDEO",
                                    "embedUrl": "https://www.youtube.com/embed/oFgg7K2tpfk",
                                    "orderIndex": 3
                                }
                            ],
                            "layoutType": "SINGLE_COLUMN"
                        }
                    },
                    {
                        "id": "9b3a1889-e9e9-4b6b-87e0-a0c57834f7ca",
                        "title": "2. Các phép toán cơ bản",
                        "pageType": "CONTENT",
                        "contentPage": {
                            "blocks": [
                                {
                                    "id": "530f5663-8453-4daf-a3b7-0dc0875b62f8",
                                    "type": "IMAGE",
                                    "imageUrl": "https://pub-d9efd09f55ef46ada4a3be608c38264b.r2.dev/images/c8d02204-ee3a-47b3-9365-97e48b8dcdec-bien-trong-python.jpg",
                                    "orderIndex": 1
                                }
                            ],
                            "layoutType": "SINGLE_COLUMN"
                        }
                    },
                    {
                        "id": "7512dc37-827e-4cdb-a079-d5f845fa48f3",
                        "title": "Kiểm tra ",
                        "pageType": "QUIZ",
                        "quizPage": {
                            "questions": [
                                {
                                    "id": "e0e99803-ac31-4c20-ac56-bd533d8fdbee",
                                    "title": "Python là ngôn ngữ bậc cao",
                                    "orderIndex": 1,
                                    "promptHtml": "<p>New question...</p>",
                                    "questionType": "TRUE_FALSE",
                                    "correctAnswer": true
                                },
                                {
                                    "id": "c7cce2c8-efe7-4b3c-a7e4-c6e94cd8f184",
                                    "options": [
                                        {
                                            "id": "9d011ba9-7f84-4014-a759-e97247923b44",
                                            "isCorrect": false,
                                            "labelHtml": "<p>Hướng dẫn cách tạo biến trong Python.</p>"
                                        },
                                        {
                                            "id": "50d2b6cc-a0c8-4454-b568-465375751e90",
                                            "isCorrect": true,
                                            "labelHtml": "<p>Yêu cầu tạo câu hỏi kiểm tra.</p>"
                                        },
                                        {
                                            "id": "a81ab996-8d3b-46bd-b9f6-924fe988278d",
                                            "isCorrect": false,
                                            "labelHtml": "<p>Mô tả một ứng dụng của Python.</p>"
                                        },
                                        {
                                            "id": "2cff3ee7-6fa4-4ef6-ad26-b3bf34a33727",
                                            "isCorrect": false,
                                            "labelHtml": "<p>Giải thích về cú pháp cơ bản.</p>"
                                        }
                                    ],
                                    "orderIndex": 2,
                                    "promptHtml": "<p>Dựa vào nội dung được cung cấp trên \"Page 3\", mục đích chính của văn bản là gì?</p>",
                                    "questionType": "MCQ_SINGLE",
                                    "explanationHtml": "<p>Nội dung văn bản là một câu lệnh trực tiếp: \"Tạo cho tôi câu hỏi về nội dung bài học này để sinh viên ứng dụng\", cho thấy mục đích rõ ràng là yêu cầu tạo ra câu hỏi.</p>"
                                },
                                {
                                    "id": "11f88c11-92ea-4b05-9788-657eb486cc22",
                                    "options": [
                                        {
                                            "id": "ff1618e7-74d3-4eb3-8ff5-612dd24550d1",
                                            "isCorrect": true,
                                            "labelHtml": "<p>Số thứ tự trang</p>"
                                        },
                                        {
                                            "id": "9f83b51d-894c-4d33-9807-063b6f48b0d5",
                                            "isCorrect": true,
                                            "labelHtml": "<p>Một yêu cầu hoặc mệnh lệnh</p>"
                                        },
                                        {
                                            "id": "d8a6603f-5b7a-456d-8c03-20dc48694f8b",
                                            "isCorrect": false,
                                            "labelHtml": "<p>Mã code ví dụ</p>"
                                        },
                                        {
                                            "id": "690b2099-1d51-4fff-b053-aa4eb6f905f0",
                                            "isCorrect": false,
                                            "labelHtml": "<p>Định nghĩa về kiểu dữ liệu</p>"
                                        }
                                    ],
                                    "orderIndex": 3,
                                    "promptHtml": "<p>Những yếu tố nào sau đây xuất hiện trong nội dung bài học được cung cấp?</p>",
                                    "questionType": "MCQ_MULTIPLE",
                                    "explanationHtml": "<p>Nội dung bài học bao gồm \"Page 3\" (số thứ tự trang) và câu \"Tạo cho tôi câu hỏi...\" (một yêu cầu). Nội dung không chứa bất kỳ mã code hay định nghĩa nào.</p>"
                                },
                                {
                                    "id": "e36f4c0a-cbae-4ccb-a8bc-477752934773",
                                    "orderIndex": 4,
                                    "promptHtml": "<p>Đúng hay Sai: Nội dung bài học trên \"Page 3\" cung cấp ít nhất một ví dụ về cách ứng dụng kiến thức cho sinh viên.</p>",
                                    "questionType": "TRUE_FALSE",
                                    "correctAnswer": false,
                                    "explanationHtml": "<p>Nội dung được cung cấp chỉ là một yêu cầu tạo câu hỏi, chưa hề có bất kỳ nội dung bài học hay ví dụ nào để sinh viên ứng dụng.</p>"
                                },
                                {
                                    "id": "a0b150a5-aea5-4bf6-9045-9a0d6bf688aa",
                                    "charLimit": 120,
                                    "orderIndex": 5,
                                    "promptHtml": "<p>Theo nội dung được cung cấp, đối tượng nào sẽ \"ứng dụng\" các câu hỏi được tạo ra?</p>",
                                    "questionType": "SHORT_ANSWER",
                                    "explanationHtml": "<p>Văn bản yêu cầu tạo câu hỏi \"...để sinh viên ứng dụng\", chỉ rõ đối tượng hướng đến là \"sinh viên\".</p>",
                                    "acceptableAnswers": [
                                        "sinh viên"
                                    ]
                                },
                                {
                                    "id": "7d9ab5ea-3cb6-433f-864e-920f0eb70fff",
                                    "answers": [
                                        "bài học"
                                    ],
                                    "orderIndex": 6,
                                    "promptHtml": "<p>null</p>",
                                    "questionType": "FILL_IN_THE_BLANK",
                                    "sentenceHtml": "Tạo cho tôi câu hỏi về nội dung [BLANK] này để sinh viên ứng dụng.",
                                    "explanationHtml": "<p>Dựa trên câu văn đầy đủ trong bài học \"Tạo cho tôi câu hỏi về nội dung bài học này để sinh viên ứng dụng\", từ còn thiếu trong ô trống là \"bài học\".</p>"
                                },
                                {
                                    "id": "02a82201-f8ac-42ed-89ad-17891fced82e",
                                    "pairs": [
                                        {
                                            "id": "9d144a31-65b8-4946-b912-d8ec3cd60c26",
                                            "left": "Tạo cho tôi câu hỏi",
                                            "right": "Hành động chính được yêu cầu"
                                        },
                                        {
                                            "id": "022658a3-5f8f-45bd-b31e-dcfba8c1a4b0",
                                            "left": "về nội dung bài học này",
                                            "right": "Phạm vi hoặc chủ đề của câu hỏi"
                                        },
                                        {
                                            "id": "27ad6099-ad9b-4c65-8622-9fae1fd1f9a3",
                                            "left": "để sinh viên ứng dụng",
                                            "right": "Mục đích cuối cùng của yêu cầu"
                                        }
                                    ],
                                    "orderIndex": 7,
                                    "promptHtml": "<p>Hãy ghép mỗi phần của câu yêu cầu với mô tả chức năng tương ứng của nó.</p>",
                                    "questionType": "MATCHING",
                                    "explanationHtml": "<p>Phân tích câu yêu cầu trong bài học cho thấy: \"Tạo cho tôi câu hỏi\" là hành động chính, \"về nội dung bài học này\" xác định phạm vi, và \"để sinh viên ứng dụng\" nêu rõ mục đích.</p>"
                                }
                            ],
                            "passingScore": 80,
                            "attemptAllowed": 1
                        }
                    }
                ],
                "title": "Chương 1: Biến và Kiểu Dữ Liệu Cơ Bản",
                "description": "Introduction section"
            }
        ],
        "description": "Mô tả: Khóa học tinh gọn nhất dành cho người mới bắt đầu, tập trung vào cú pháp cốt lõi để bạn có thể viết chương trình đầu tiên ngay lập tức.",
        "coverImageUrl": "https://pub-d9efd09f55ef46ada4a3be608c38264b.r2.dev/images/dcbb5554-5ed5-4657-b1e0-51902eebd7ba-python-co-ban_b80bca9b238b4615b94541de28af00ae.png"
    },
    "editorVersion": "editor-state-v2",
    "editorStatus": "DRAFT",
    "createdAt": "2026-04-13T15:00:07.814512Z",
    "courseUserId": 7,
    "thumbnail": null,
    "sections": []
}
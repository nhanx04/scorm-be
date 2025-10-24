# Cách tạo gói SCORM

## Tổng quan kỹ thuật về tiêu chuẩn SCORM

**SCORM** quy định rằng nội dung học tập phải:

- Được đóng gói trong một tệp ZIP.
- Được mô tả trong một tệp XML.
- Giao tiếp thông qua JavaScript.
- Tuân theo trình tự (sequence) được định nghĩa bằng các quy tắc trong XML.

## Cấu trúc của SCORM

SCORM bao gồm ba phần tiêu chuẩn con (sub-specifications):

1. Phần Content Packaging: xác định cách nội dung được đóng gói và mô tả. Phần này chủ yếu dựa trên XML.
2. Phần Run-Time: xác định cách nội dung được khởi chạy và cách nó giao tiếp với hệ thống LMS. Phần này chủ yếu dựa trên ECMAScript (JavaScript).
3. Phần Sequencing: xác định cách người học có thể di chuyển giữa các phần của khóa học (các SCOs). Phần này được định nghĩa bằng các quy tắc và thuộc tính được viết trong XML.

## Content Packaging - Đóng gói nội dung

Mô hình đóng gói nội dung của SCORM quy định rằng nội dung phải được đóng gói trong một thư mục độc lập hoặc tệp ZIP. Gói này được gọi là Package Interchange File (PIF). PIF phải luôn chứa một tệp XML tên là `imsmanifest.xml` (còn gọi là “tệp manifest”) nằm ở thư mục gốc.Tệp manifest chứa toàn bộ thông tin mà LMS cần để phân phối nội dung.

Bên trong manifest:

- Khóa học được chia thành một hoặc nhiều phần, gọi là SCOs (Sharable Content Objects).
- Các SCO có thể được kết hợp thành cấu trúc dạng cây, đại diện cho toàn bộ khóa học — gọi là “activity tree”.
- Manifest chứa biểu diễn XML của cây hoạt động, thông tin về cách khởi chạy từng SCO, và (tùy chọn) metadata mô tả khóa học và các phần của nó.

Trọng tâm của đặc tả đóng gói nội dung SCORM là tệp manifest của khóa học. Tệp manifest là một tệp XML mô tả toàn bộ nội dung của khóa học.

Nó bao gồm một số phần quan trọng sau:

### Resources - Tài nguyên

Resources là danh sách các “thành phần” tạo nên khóa học. Có hai loại tài nguyên: SCOs và Assets.

- Asset là một tập hợp gồm một hoặc nhiều tệp tạo thành một đơn vị logic. Các Asset có thể:
    - Là những đơn vị học độc lập (các phần củ khoá học), hoặc
    - Là các nhóm tệp được tái sử dụng ở nhiều phần khác nhau của khóa học (ví dụ: một bộ hình ảnh thương hiệu chung).
- SCO (Sharable Content Object) là một đơn vị học cũng được cấu thành từ một hoặc nhiều tệp. SCO gần như luôn là phần mang tính giảng dạy của khóa học.

Sự khác biệt chính giữa SCO và Asset: SCO có thể giao tiếp với hệ thống LMS (Learning Management System). Asset chỉ là nội dung tĩnh, được trình bày cho người học mà không có giao tiếp với LMS. Bất kỳ tài nguyên nào có thể được người học khởi chạy đều chứa con trỏ (pointer) đến trang mà LMS sẽ chuyển hướng người học để mở tài nguyên đó. Mỗi Resource cũng cần chứa danh sách đầy đủ tất cả các tệp cần thiết cho hoạt động của nó, để khi được di chuyển sang môi trường mới, nó vẫn có thể hoạt động bình thường.

### Organizations trong SCORM

Organizations là các nhóm logic của những phần nội dung (resources) trong một khóa học, được sắp xếp theo cấu trúc phân cấp.

Một tệp manifest có thể chứa nhiều hơn một organization cho cùng một nội dung — ví dụ để trình bày nội dung khác nhau cho các đối tượng người học khác nhau. Tuy nhiên, thông thường chỉ có một organization duy nhất, được gọi là organization mặc định (default organization).

Các organization luôn có cấu trúc phân cấp dạng cây (tree structure). Các nút (nodes) trong cây này được gọi là “activities” (khi nói trong ngữ cảnh điều hướng – sequencing) hoặc “items” (khi nói trong ngữ cảnh đóng gói nội dung – content packaging).

Mỗi item có thể chứa các item con (child items) bên trong nó. Khi một item có các phần tử con, nó được gọi là một “aggregation” hoặc “cluster”.

Những item không có phần tử con bắt buộc phải tham chiếu đến một resource (tài nguyên). Chính tài nguyên này sẽ được cung cấp cho người học khi họ chọn item đó trong khóa học.

Ngược lại, những item có phần tử con thì không được phép tham chiếu đến resource, vì chúng chỉ đóng vai trò là vật chứa (container) cho các item khác.

Điều này có thể được hình dung giống như cấu trúc thư mục trên máy tính:

- Một item có thể là thư mục (folder) hoặc tệp (file), nhưng không thể đồng thời là cả hai.
- Thư mục có thể chứa các thư mục hoặc tệp khác, nhưng không được để trống (“empty folders” là không hợp lệ).

### Metadata - Siêu dữ liệu

Mỗi phần của tệp manifest đều có thể được mô tả chi tiết bằng cách gắn kèm siêu dữ liệu (metadata) với nó.
Siêu dữ liệu trong SCORM được ghi lại theo một định dạng chuẩn hóa rõ ràng gọi là “learning object metadata” (LOM).

LOM bao gồm nhiều trường được định nghĩa sẵn để mô tả nội dung học tập.
SCORM cũng cho phép mở rộng LOM, giúp các tổ chức có thể chỉ định thêm các siêu dữ liệu bổ sung.

Siêu dữ liệu có thể được áp dụng cho hầu như mọi phần trong tệp manifest — ví dụ, có thể áp dụng cho toàn bộ khóa học, cho từng mục riêng lẻ, hoặc thậm chí cho từng tài nguyên và tệp riêng biệt nhằm tăng khả năng tái sử dụng của chúng.

Bên trong tệp manifest, siêu dữ liệu có thể được:

- Chèn trực tiếp trong XML (được khuyến nghị khi lượng siêu dữ liệu nhỏ, đặc biệt ở cấp độ khóa học).
- Tham chiếu tới tệp siêu dữ liệu bên ngoài (được khuyến nghị khi có lượng lớn siêu dữ liệu chi tiết).

Siêu dữ liệu thường là tùy chọn, tuy nhiên SCORM 1.2 có đặt ra một số giới hạn tối thiểu đối với tập dữ liệu cần phải được xác định nếu có chỉ định bất kỳ dữ liệu nào.

Lượng siêu dữ liệu SCORM phù hợp cần sử dụng sẽ thay đổi đáng kể tùy theo mục đích sử dụng nội dung, tuổi thọ dự kiến của khóa học, và khả năng nội dung đó được tái sử dụng trong tương lai.

### Sequencing - Trình tự học

Trong SCORM 2004, mỗi hoạt động có thể được gán một tập hợp các quy tắc trình tự. Các quy tắc này được mã hóa bằng XML trong tệp manifest của khóa học. SCORM được thiết kế sao cho một khóa học đơn giản chỉ bao gồm các asset (tài nguyên tĩnh) thì không cần phải chỉ định thêm quy tắc trình tự nào ngoài các mặc định. Tuy nhiên, trên thực tế, có một số thiết lập mặc định nên được ghi đè (override) — trừ khi đó là khóa học rất đơn giản.

### Packaging the content (Đóng gói nội dung)

Khi nội dung đã được biểu diễn dưới dạng XML, nó sẽ được lưu vào một tệp có tên là `imsmanifest.xml`. Tệp manifest này luôn phải nằm ở thư mục gốc của nội dung.

Để tuân thủ hoàn toàn theo chuẩn SCORM, nội dung cũng nên bao gồm bộ tệp định nghĩa lược đồ XML (.xsd và .dtd) mô tả cú pháp XML được sử dụng trong tệp manifest, bao gồm cả các phần tử mở rộng (nếu có).

Nội dung sau đó có thể được phân phối theo hai cách:

- Dưới dạng thư mục đơn giản (ví dụ như trên CD).
- Được nén trong một tệp ZIP.

Khi nội dung được đặt trong tệp ZIP, nó được gọi là “package interchange file” (PIF). Các PIF là định dạng phổ biến nhất để phân phối nội dung SCORM.

Một nguyên tắc quan trọng trong việc đóng gói nội dung là: Lý tưởng nhất, mọi thứ cần thiết để chạy khoá học nên được đóng gói hoàn chỉnh trong tệp PIF. SCORM khuyến khích mạnh tính di động và khả năng tái sử dụng. Để đạt được điều đó, mọi tệp cần thiết cho khoá học phải được bao gồm trong PIF và được liệt kê trong tệp manifest. Ngoài ra, nhà phát triển nội dung nên tránh sử dụng mã server-size hoặc các phụ thuộc khác như cơ sở dữ liệu. Việc sử dụng công cụ hoặc phụ thuộc bên ngoài vẫn được SCORM cho phép, nhưng thông lệ trong ngành là nên hạn chế tối đa khi có thể.

Tóm lại, một gói SCORM chuẩn là một tệp ZIP chứa:

- **File imsmanifest.xml** ở gốc,
- Các **file nội dung (HTML, media, JS, CSS...)** được tổ chức trong các thư mục tùy ý,
- Các **file schema (.xsd)** cần thiết (ví dụ: *imscp_rootv1p1p2.xsd*, *imsmd_rootv1p2p1.xsd*, *adlcp_rootv1p2.xsd* đối với SCORM 1.2) ở gốc (những file này được nhắc đến trong phần đầu của `imsmanifest.xml`)

Khi nén ZIP, đảm bảo không tạo thêm một thư mục mẹ bao ngoài - tất cả nội dung phải nằm trực tiếp trong file zip (nếu không, LMS có thể không tìm thấy `imsmanifest.xml` do nó bị nằm trong thư mục con). Một số LMS có thể bỏ qua lỗi này, nhưng nhiều LMS yêu cầu đúng cấu trúc như trên.

## 🧩 Tạo file `imsmanifest.xml`

### 📌 Vai trò:

- Là trái tim của gói SCORM, giúp LMS hiểu cấu trúc khoá học và cách hiển thị nội dung.
- Tuân theo chuẩn IMS Content Packaging 1.1.2.

### 1. Metadata - Siêu dữ liệu

- Mô tả thông tin khoá học: tiêu đề, mô tả, từ khoá, phiên bản,…
- Có thể:
    - Nhúng trực tiếp trong manifest.
    - Tham chiếu tới file XML bên ngoài (chỉ chọn 1 cách, không dùng đồng thời).
- Một số phần tử metadata bắt buộc theo chuẩn SCORM 1.2.

### 2. Organizations - Cấu trúc khoá học

- Định nghĩa cây nội dung gồm các SCO (Sharable Content Object) hoặc Asset.
- Gồm ít nhất một thẻ `<organizations>` (có thuộc tính `default`).
- Mỗi `<organizations>` chứa các <item> lồng nhau, biểu diễn menu hoặc mục lục khoá học.
- Mỗi `<item>` phải có `identifierref` trỏ đến một `<resource>` trong phần resources.
- SCORM yêu cầu ít nhất một `<organizations>`, nếu không LMS sẽ không hiển thị nội dung.

### 3. Resources - Tài nguyên

- Khai báo tất cả SCOs và assets trong gói.
- Mỗi `<resource>` đại diện cho một đơn vị nội dung có thể khởi chạy.
- Thuộc tính chính:
    - `identifier` - ID duy nhất.
    - `type="webcontent"` – kiểu nội dung web.
    - `href="..."` – file HTML chính.
    - `adlcp:scormtype="sco"` hoặc `"asset"`.
- Bên trong `<resource>`: danh sách các `<file>` mô tả toàn bộ file liên quan (HTML, JS, CSS, ảnh,…)
    - Best practice: liệt kê đầy đủ tất cả file để đảm bảo hoạt động ổn định.
- Nếu nhiều SCO dùng chung file, có thể:
    - Tạo một `<resource>` riêng loại asset.
    - Dùng `<dependency>` để tham chiếu (tránh trùng lặp).

### 4. Sub-manifests (tuỳ chọn)

- Cho phép chứa manifest lồng nhau để đóng gói nội dung lớn hoặc tái sử dụng giữa các khoá học.
- Không cần dùng gói SCORM cơ bản.

### Best practices:

- Tham khảo ví dụ SCORM chính thức, như “Golf Example” từ [scorm.com](http://scorm.com).
- Chỉnh sửa các trường title, identifier, href cho phù hợp nội dung.
- Kiểm tra file bằng XML Editor để xác minh đúng schema XSD.

## Thêm câu hỏi trắc nghiệm vào SCO (SCORM 1.2)

### 🎯 Mục tiêu:

Ghi nhận điểm và trạng thái học viên thông qua SCORM API khi làm quiz trắc nghiệm (multiple-choice).

### 1. Cấu trúc quiz cơ bản

- Tạo form câu hỏi bằng HTML + JavaScript.
- Khi học viên chọn đáp án → lưu điểm tạm vào biến JS (ví dụ: 0, 60, 100).
- Khi hoàn thành hoặc thoát SCO → ghi điểm lên LMS qua SCORM API.

### 2. Các lệnh SCORM chính

- `LMSSetValue("cmi.core.score.raw", <điểm>)` → Gán điểm đạt được.
    - Nếu `mastery score` có trong manifest → LMS tự xác định Passed/Failed.
- `LMSSetValue("cmi.core.lesson_status", "passed"/"failed")` → Cập nhật trạng thái học viên.
    - Hoặc `"completed"/"incomplete"` nếu chỉ theo dõi hoàn thành.
- `LMSCommit("")` → Gửi dữ liệu lên LMS (nên gọi sau mỗi lần ghi điểm).
- `LMSFinish("")` → Kết thúc SCO, chốt dữ liệu khi rời trang.

### 3. Lưu dữ liệu khi thoát trang

- Khi SCO unload, kiểm tra nếu đã có điểm:

```jsx
SCOSetValue("cmi.core.score.raw", score);
SCOCommit();
```

- Đảm bảo điểm được ghi lại và gửi lên LMS trước khi kết thúc.

### 4. Dữ liệu mở rộng (tuỳ chọn)

- Có thể lưu câu trả lời chi tiết qua cmi.interactions (SCORM 1.2 hỗ trợ).
- Với quiz ngắn → chỉ cần lưu điểm và trạng thái là đủ.

### 5. Lưu ý

- Nếu `mastery score` có trong manifest → LMS **tự động suy ra** Passed/Failed.
- Nếu không, phải tự đặt **`lesson_status`**.
- Trong SCORM 1.2: “hoàn thành” và “đậu” là cùng một trạng thái.
- Trong SCORM 2004: hai trạng thái này được tách riêng (completion vs success).

Tóm gọn:

> Quiz → JavaScript tính điểm → SCORM API (SetValue → Commit → Finish) → LMS ghi nhận điểm & trạng thái học viên.
> 

## ⚙️ Tích hợp nội dung HTML/JS với SCORM API

### 🎯 Mục tiêu:

Giúp SCO giao tiếp với LMS (ghi điểm, lưu trạng thái, đồng bộ dữ liệu học tập).

### 1. API Wrapper

- Thường có sẵn file `scorm_api_wrapper.js` (hoặc dùng thư viện như pipwerks SCORM API Wrapper).
- Giúp đơn giản hóa việc gọi SCORM API (`Initialize()`, `Finish()`, `GetValue()`, `SetValue()`...).

### 2. Quy trình hoạt động của SCO

**Khởi tạo**

Khi trang tải (`window.onload` hoặc `<body onload>`):

- Tìm đối tượng API của LMS (qua `window.parent` hoặc `window.opener`).
- Gọi `LMSInitialize("")` để mở phiên giao tiếp SCORM.

**Trao đổi dữ liệu**

- Ghi dữ liệu: `LMSSetValue("cmi.core.score.raw", <điểm>)`.
- Đọc dữ liệu: `LMSGetValue("cmi.core.student_name")`, `LMSGetValue("cmi.core.lesson_status")`, v.v.
- Sau mỗi lần ghi, gọi `LMSCommit("")` để lưu tạm thời dữ liệu lên LMS.
- Một số trường CMI thường dùng:
    - `cmi.core.lesson_status` → trạng thái học (completed/passed/failed).
    - `cmi.core.score.raw` → điểm số.
    - `cmi.core.lesson_location` → vị trí đánh dấu.
    - `cmi.core.session_time` → thời gian học.

**Kết thúc**

Khi người học hoàn tất hoặc đóng trang:

- Gọi `LMSFinish("")` để kết thúc phiên SCORM.
- Thường đặt trong `window.onbeforeunload` để gửi dữ liệu cuối cùng.
- Sau khi `LMSFinish` được gọi → không thể `SetValue`/`GetValue` nữa trừ khi mở lại SCO.

### 3. Ví dụ tích hợp đơn giản

```html
<script src="scorm_api_wrapper.js"></script>
<script>
  window.onload = function() {
    scorm = new ScormAPI();
    scorm.initialize();  // Bắt đầu phiên SCORM
  };
  window.onbeforeunload = function() {
    scorm.terminate();   // Kết thúc phiên SCORM
  };
  function completeCourse() {
    scorm.set("cmi.core.lesson_status", "completed");
    scorm.set("cmi.core.score.raw", 100);
    scorm.save();  // Lưu dữ liệu
    alert("Course Completed!");
  }
</script>
```

### 4. Best practices

- Luôn gọi `LMSInitialize` trước khi ghi dữ liệu.
- Luôn gọi `LMSFinish` khi kết thúc hoặc rời trang.
- Dùng `LMSCommit` thường xuyên để tránh mất dữ liệu khi người học thoát đột ngột.
- Một số trình duyệt có thể không gửi kịp dữ liệu khi unload nhanh, nên nên commit trước khi thoát.

Tóm gọn:

> HTML/JS của SCO → gọi SCORM API (Initialize → Set/Get → Commit → Finish) → LMS ghi nhận điểm, tiến trình, trạng thái học viên.
> 

## 📦 **Đóng gói nội dung thành tệp SCORM (.zip)**

### 🎯 Mục tiêu:

Tạo gói SCORM hoàn chỉnh (chuẩn SCORM 1.2/2004) có thể upload lên LMS.

### 1. Cấu trúc và vị trí file

- `imsmanifest.xml` ****phải nằm ở thư mục gốc của file `.zip` (không nằm trong thư mục con).
- Bao gồm toàn bộ file nội dung: `.html`, `.js`, `.css`, hình ảnh, video, audio, v.v.
- Nếu có, thêm các file schema (.xsd) của IMS/SCORM để LMS kiểm tra tính hợp lệ.

### 2. Quy tắc khi nén file

- Chỉ chọn các file và thư mục bên trong → không nén thêm một thư mục bao ngoài.
- Khi mở `.zip` → phải thấy ngay `imsmanifest.xml` ****ở root cùng các thư mục nội dung.
- Tránh đặt manifest trong thư mục con → nhiều LMS không tìm thấy file và báo lỗi import.

### **3. Kiểm thử gói SCORM**

- Kiểm tra bằng:
    - SCORM Cloud
    - SCORM Test Suite
    - LMS nội bộ (import thử trực tiếp).
- Đảm bảo LMS nhận diện đúng:
    - Tên khóa học, cấu trúc, SCOs.
    - Điểm & trạng thái (completed/passed).

### 4. Khi có lỗi

Kiểm tra lại file `imsmanifest.xml`:

- Đường dẫn file (`href`)
- Chính tả tag XML
- Phiên bản schema (`schemaVersion`)
- Tính hợp lệ XSD.

Tóm gọn:

> Gói SCORM = .zip chứa imsmanifest.xml ở root + toàn bộ file nội dung + schema XSD → test trên SCORM Cloud trước khi import LMS chính thức.
>
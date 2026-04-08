-- ============================================
-- 2. USERS
-- USER(UserID, Fname, Minit, Name, Email, Password_Hash, Avatar_Url,
--      Is_Active, Last_Login_At, Created_At, Updated_At)
-- ============================================
CREATE TABLE users (
    userid          BIGSERIAL,
    fname           VARCHAR(100),
    lname           VARCHAR(255),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    avatar_url      TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (userid),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ============================================
-- 3. ORGANIZATION
-- ORGANIZATION(OrgID, Org_Name, Description, Max_Authors, Logo_Media_ID,
--              Org_OwnerID, Created_At, Org_UserID)
-- ============================================
CREATE TABLE organization (
    orgid           BIGSERIAL,
    org_name        VARCHAR(255),
    description     TEXT,
    max_authors     INT,
    logo_media_id   BIGINT,
    org_ownerid     BIGINT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    org_userid      BIGINT,
    CONSTRAINT pk_organization PRIMARY KEY (orgid),
    CONSTRAINT fk_organization_org_owner
        FOREIGN KEY (org_ownerid) REFERENCES users(userid),
    CONSTRAINT fk_organization_org_user
        FOREIGN KEY (org_userid)  REFERENCES users(userid)
);

-- ============================================
-- 4. MEMBERSHIP
-- MEMBERSHIP(OrgID, UserID, Org_Role, Invited_At, Joined_At, Status)
-- ============================================
CREATE TABLE membership (
    orgid       BIGINT,
    userid      BIGINT,
    org_role    VARCHAR(50),
    invited_at  TIMESTAMPTZ,
    joined_at   TIMESTAMPTZ,
    status      VARCHAR(50),
    CONSTRAINT pk_membership PRIMARY KEY (orgid, userid),
    CONSTRAINT fk_membership_org
        FOREIGN KEY (orgid)  REFERENCES organization(orgid),
    CONSTRAINT fk_membership_user
        FOREIGN KEY (userid) REFERENCES users(userid)
);

-- ============================================
-- 5. COURSE
-- COURSE(CourseID, Title, Passing_Score, Attempt_Limit, Duration_Min,
--        Status, Last_Published_At, Updated_At, Extra_Infor, Created_At,
--        Course_UserID)
-- ============================================
CREATE TABLE course (
    courseid            BIGSERIAL,
    title               VARCHAR(255),
    passing_score       NUMERIC(5,2),
    attempt_limit       INT,
    duration_min        INT,
    status              VARCHAR(50) NOT NULL DEFAULT 'Draft',
    tags                VARCHAR(50)[],
    is_favorite         BOOLEAN DEFAULT FALSE,
    last_published_at   TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    extra_infor         JSONB,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    course_userid       BIGINT NOT NULL,
    CONSTRAINT pk_course PRIMARY KEY (courseid),
    CONSTRAINT fk_course_user
        FOREIGN KEY (course_userid) REFERENCES users(userid)
);

-- ============================================
-- 6. SCORM_EXPORT_CONFIG
-- SCORM_EXPORT_CONFIG(Scorm_ConfigID, Extra_Config, Config_CourseID)
-- ============================================
CREATE TABLE scorm_export_config (
    scorm_configid   BIGSERIAL,

    -- Export & runtime config (SCORM version, navigation, tracking, etc.)
    extra_config     JSONB,

    -- Theme / UI customization (design tokens)
    -- Store as JSONB to allow maximum flexibility for future additions.
    -- Suggested structure is documented in the backend layer (DTO/validation).
    theme_config     JSONB,

    config_courseid  BIGINT NOT NULL,
    CONSTRAINT pk_scorm_export_config PRIMARY KEY (scorm_configid),
    CONSTRAINT fk_scorm_export_config_course
        FOREIGN KEY (config_courseid) REFERENCES course(courseid)
);

-- ============================================
-- 7. SCORM_PACKAGE
-- SCORM_PACKAGE(Scorm_PackageID, Package_Name, Package_Type, Zip_File_Path,
--               Package_CourseID, Package_ConfigID, Package_UserID)
-- ============================================
CREATE TABLE scorm_package (
    scorm_packageid   BIGSERIAL,
    package_name      VARCHAR(255),
    package_type      VARCHAR(50),

    -- Output artifact
    zip_file_path     TEXT,

    -- Theme snapshot at export time (so package remains stable even if config changes later)
    theme_snapshot    JSONB,

    package_courseid  BIGINT NOT NULL,
    package_configid  BIGINT NOT NULL,
    package_userid    BIGINT NOT NULL,
    CONSTRAINT pk_scorm_package PRIMARY KEY (scorm_packageid),
    CONSTRAINT fk_scorm_package_course
        FOREIGN KEY (package_courseid) REFERENCES course(courseid),
    CONSTRAINT fk_scorm_package_config
        FOREIGN KEY (package_configid) REFERENCES scorm_export_config(scorm_configid),
    CONSTRAINT fk_scorm_package_user
        FOREIGN KEY (package_userid)   REFERENCES users(userid)
);

-- ============================================
-- 8. SCORM_ACTIVITY
-- SCORM_ACTIVITY(ActivityID, Title, Activity_Type, Order_Index, Extra_Config,
--                Parent_ActivityID, Activity_PackageID)
-- ============================================
CREATE TABLE scorm_activity (
    activityid          BIGSERIAL,
    title               VARCHAR(255),
    activity_type       VARCHAR(50),
    order_index         INT,
    extra_config        JSONB,
    parent_activityid   BIGINT,
    activity_packageid  BIGINT NOT NULL,
    CONSTRAINT pk_scorm_activity PRIMARY KEY (activityid),
    CONSTRAINT fk_scorm_activity_parent
        FOREIGN KEY (parent_activityid)  REFERENCES scorm_activity(activityid),
    CONSTRAINT fk_scorm_activity_package
        FOREIGN KEY (activity_packageid) REFERENCES scorm_package(scorm_packageid)
);

-- ============================================
-- 9. SCORM_RESOURCE
-- SCORM_RESOURCE(Scorm_ResourceID, Identifier, Href, Mime_Type, Scorm_Type,
--                Resource_PackageID)
-- ============================================
CREATE TABLE scorm_resource (
    scorm_resourceid    BIGSERIAL,
    identifier          VARCHAR(255),
    href                TEXT,
    mime_type           VARCHAR(255),
    scorm_type          VARCHAR(50),
    resource_packageid  BIGINT NOT NULL,
    CONSTRAINT pk_scorm_resource PRIMARY KEY (scorm_resourceid),
    CONSTRAINT fk_scorm_resource_package
        FOREIGN KEY (resource_packageid) REFERENCES scorm_package(scorm_packageid)
);

-- ============================================
-- 10. MY_LIBRARY
-- LIBRARY(LibraryID, Library_Name, Description, Scope_Type, Updated_At,
--         Lib_OwnerID)
-- ============================================
CREATE TABLE my_library (
    libraryid       BIGSERIAL,
    library_name    VARCHAR(255),
    description     TEXT,
    scope_type      VARCHAR(50),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    lib_ownerid     BIGINT NOT NULL,
    CONSTRAINT pk_my_library PRIMARY KEY (libraryid),
    CONSTRAINT fk_my_library_owner
        FOREIGN KEY (lib_ownerid) REFERENCES users(userid)
);

-- ============================================
-- 11. MEDIA_ASSET
-- MEDIA_ASSET(MediaID, Title, Description, Original_File_Name, Metadata,
--             Media_Type, Metadata, Updated_At, Asset_LibraryID,
--             Uploaded_At, Asset_UserID)
-- (metadata lặp 2 lần trong mapping → gộp 1 cột metadata)
-- ============================================
CREATE TABLE media_asset (
    mediaid            BIGSERIAL,
    title              VARCHAR(255),
    description        TEXT,
    original_file_name VARCHAR(255),
    metadata           JSONB,
    media_type         VARCHAR(50),
    updated_at         TIMESTAMPTZ DEFAULT NOW(),
    asset_libraryid    BIGINT NOT NULL,
    uploaded_at        TIMESTAMPTZ DEFAULT NOW(),
    asset_userid       BIGINT NOT NULL,
    CONSTRAINT pk_media_asset PRIMARY KEY (mediaid),
    CONSTRAINT fk_media_asset_library
        FOREIGN KEY (asset_libraryid) REFERENCES my_library(libraryid),
    CONSTRAINT fk_media_asset_user
        FOREIGN KEY (asset_userid)    REFERENCES users(userid)
);

-- ============================================
-- 12. IMAGE_ASSET
-- IMAGE_ASSET(MediaID, Metadata)
-- ============================================
CREATE TABLE image_asset (
    mediaid    BIGINT,
    metadata   JSONB,
    CONSTRAINT pk_image_asset PRIMARY KEY (mediaid),
    CONSTRAINT fk_image_asset_media
        FOREIGN KEY (mediaid) REFERENCES media_asset(mediaid)
);

-- ============================================
-- 13. AUDIO_ASSET
-- AUDIO_ASSET(MediaID, Metadata)
-- ============================================
CREATE TABLE audio_asset (
    mediaid    BIGINT,
    metadata   JSONB,
    CONSTRAINT pk_audio_asset PRIMARY KEY (mediaid),
    CONSTRAINT fk_audio_asset_media
        FOREIGN KEY (mediaid) REFERENCES media_asset(mediaid)
);

-- ============================================
-- 14b. VIDEO_ASSET
-- VIDEO_ASSET(MediaID, Youtube_Url)
-- (YouTube embed link only)
-- ============================================
CREATE TABLE video_asset (
    mediaid       BIGINT,
    youtube_url   TEXT NOT NULL,
    CONSTRAINT pk_video_asset PRIMARY KEY (mediaid),
    CONSTRAINT fk_video_asset_media
        FOREIGN KEY (mediaid) REFERENCES media_asset(mediaid)
);

-- ============================================
-- 14. DOCUMENT_ASSET
-- DOCUMENT_ASSET(MediaID, Metadata)
-- ============================================
CREATE TABLE document_asset (
    mediaid    BIGINT,
    metadata   JSONB,
    CONSTRAINT pk_document_asset PRIMARY KEY (mediaid),
    CONSTRAINT fk_document_asset_media
        FOREIGN KEY (mediaid) REFERENCES media_asset(mediaid)
);

-- ============================================
-- 15. THUMBNAIL_OF_COURSE
-- THUMBNAIL_OF_COURSE(Thumbnail_CourseID, Thumbnail_MediaID)
-- ============================================
CREATE TABLE thumbnail_of_course (
    thumbnail_courseid  BIGINT,
    thumbnail_mediaid   BIGINT NOT NULL,
    CONSTRAINT pk_thumbnail_of_course PRIMARY KEY (thumbnail_courseid),
    CONSTRAINT fk_thumbnail_of_course_course
        FOREIGN KEY (thumbnail_courseid) REFERENCES course(courseid),
    CONSTRAINT fk_thumbnail_of_course_media
        FOREIGN KEY (thumbnail_mediaid)  REFERENCES image_asset(mediaid)
);

-- ============================================
-- 16. QUESTION (Universal model)
-- Supports: single choice, multiple choice, true/false, fill blank, matching,
-- short answer, grouping/classification, and future question types.
-- ============================================
CREATE TABLE question (
    questionid          BIGSERIAL,

    -- Human-readable
    title               VARCHAR(255),
    instruction         TEXT,
    prompt_html         TEXT,

    -- Core typing
    -- Suggested values: MCQ_SINGLE, MCQ_MULTI, TRUE_FALSE, FILL_BLANK,
    -- MATCHING, SHORT_ANSWER, GROUPING
    question_type       VARCHAR(50) NOT NULL,

    -- Shared behavior
    -- points: score weight
    -- shuffle_options: for option-based questions
    -- case_sensitive: for text-based questions
    points              NUMERIC(8,2) DEFAULT 1,
    shuffle_options     BOOLEAN DEFAULT FALSE,
    case_sensitive      BOOLEAN DEFAULT FALSE,

    -- Extensible config (validation rules, feedback, hints, etc.)
    extra_config        JSONB,

    CONSTRAINT pk_question PRIMARY KEY (questionid)
);

-- ============================================
-- 17. QUESTION_CHOICE_OPTION (for MCQ_SINGLE / MCQ_MULTI / TRUE_FALSE)
-- One question can have many options.
-- is_correct supports multiple correct answers.
-- ============================================
CREATE TABLE question_choice_option (
    optionid        BIGSERIAL,
    questionid      BIGINT NOT NULL,
    order_index     INT,
    content_html    TEXT,
    is_correct      BOOLEAN DEFAULT FALSE,
    score_fraction  NUMERIC(6,4),

    CONSTRAINT pk_question_choice_option PRIMARY KEY (optionid),
    CONSTRAINT fk_q_choice_option_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

-- ============================================
-- 18. QUESTION_TRUE_FALSE (optional helper table)
-- If you want strict T/F semantics separate from general options.
-- You can keep using question_choice_option with 2 options instead.
-- ============================================
CREATE TABLE question_true_false (
    questionid     BIGINT,
    correct_value  BOOLEAN NOT NULL,

    CONSTRAINT pk_question_true_false PRIMARY KEY (questionid),
    CONSTRAINT fk_question_true_false_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

-- ============================================
-- 19. QUESTION_FILL_BLANK
-- prompt_html contains placeholders. Each blank has accepted answers.
-- ============================================
CREATE TABLE question_fill_blank (
    questionid      BIGINT,
    -- if TRUE: blanks must be filled in a specific order
    ordered_blanks  BOOLEAN DEFAULT FALSE,

    CONSTRAINT pk_question_fill_blank PRIMARY KEY (questionid),
    CONSTRAINT fk_question_fill_blank_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_blank (
    blankid      BIGSERIAL,
    questionid   BIGINT NOT NULL,
    blank_key    VARCHAR(100),
    order_index  INT,

    CONSTRAINT pk_question_blank PRIMARY KEY (blankid),
    CONSTRAINT fk_question_blank_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_blank_answer (
    answerid     BIGSERIAL,
    blankid      BIGINT NOT NULL,
    answer_text  TEXT NOT NULL,
    -- allow alternative spellings, synonyms, etc.
    match_rule   VARCHAR(50),

    CONSTRAINT pk_question_blank_answer PRIMARY KEY (answerid),
    CONSTRAINT fk_question_blank_answer_blank
        FOREIGN KEY (blankid) REFERENCES question_blank(blankid)
);

-- ============================================
-- 20. QUESTION_MATCHING
-- Pair left items to right items (can support many-to-one if needed).
-- ============================================
CREATE TABLE question_matching (
    questionid  BIGINT,

    CONSTRAINT pk_question_matching PRIMARY KEY (questionid),
    CONSTRAINT fk_question_matching_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_matching_left (
    leftid       BIGSERIAL,
    questionid   BIGINT NOT NULL,
    order_index  INT,
    content_html TEXT,

    CONSTRAINT pk_question_matching_left PRIMARY KEY (leftid),
    CONSTRAINT fk_q_matching_left_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_matching_right (
    rightid      BIGSERIAL,
    questionid   BIGINT NOT NULL,
    order_index  INT,
    content_html TEXT,

    CONSTRAINT pk_question_matching_right PRIMARY KEY (rightid),
    CONSTRAINT fk_q_matching_right_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_matching_pair (
    pairid    BIGSERIAL,
    questionid BIGINT NOT NULL,
    leftid    BIGINT NOT NULL,
    rightid   BIGINT NOT NULL,
    score     NUMERIC(8,2),

    CONSTRAINT pk_question_matching_pair PRIMARY KEY (pairid),
    CONSTRAINT fk_q_matching_pair_question
        FOREIGN KEY (questionid) REFERENCES question(questionid),
    CONSTRAINT fk_q_matching_pair_left
        FOREIGN KEY (leftid) REFERENCES question_matching_left(leftid),
    CONSTRAINT fk_q_matching_pair_right
        FOREIGN KEY (rightid) REFERENCES question_matching_right(rightid)
);

-- ============================================
-- 21. QUESTION_SHORT_ANSWER
-- Store expected answers and validation rules.
-- ============================================
CREATE TABLE question_short_answer (
    questionid       BIGINT,
    min_length       INT,
    max_length       INT,

    CONSTRAINT pk_question_short_answer PRIMARY KEY (questionid),
    CONSTRAINT fk_question_short_answer_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_short_answer_expected (
    expectedid    BIGSERIAL,
    questionid    BIGINT NOT NULL,
    answer_text   TEXT NOT NULL,
    match_rule    VARCHAR(50),

    CONSTRAINT pk_question_short_answer_expected PRIMARY KEY (expectedid),
    CONSTRAINT fk_q_short_answer_expected_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

-- ============================================
-- 22. QUESTION_GROUPING (classification)
-- Items must be assigned into groups.
-- ============================================
CREATE TABLE question_grouping (
    questionid  BIGINT,

    CONSTRAINT pk_question_grouping PRIMARY KEY (questionid),
    CONSTRAINT fk_question_grouping_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_group (
    groupid      BIGSERIAL,
    questionid   BIGINT NOT NULL,
    order_index  INT,
    title        VARCHAR(255),
    description  TEXT,

    CONSTRAINT pk_question_group PRIMARY KEY (groupid),
    CONSTRAINT fk_question_group_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_group_item (
    itemid       BIGSERIAL,
    questionid   BIGINT NOT NULL,
    order_index  INT,
    content_html TEXT,

    CONSTRAINT pk_question_group_item PRIMARY KEY (itemid),
    CONSTRAINT fk_question_group_item_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

CREATE TABLE question_group_item_answer (
    answerid   BIGSERIAL,
    itemid     BIGINT NOT NULL,
    groupid    BIGINT NOT NULL,

    CONSTRAINT pk_question_group_item_answer PRIMARY KEY (answerid),
    CONSTRAINT fk_q_group_item_answer_item
        FOREIGN KEY (itemid) REFERENCES question_group_item(itemid),
    CONSTRAINT fk_q_group_item_answer_group
        FOREIGN KEY (groupid) REFERENCES question_group(groupid)
);

-- ============================================
-- 23. MEDIA_OF_QUESTION
-- ============================================
CREATE TABLE media_of_question (
    questionid  BIGINT,
    mediaid     BIGINT,
    CONSTRAINT pk_media_of_question PRIMARY KEY (questionid, mediaid),
    CONSTRAINT fk_media_of_question_question
        FOREIGN KEY (questionid) REFERENCES question(questionid),
    CONSTRAINT fk_media_of_question_media
        FOREIGN KEY (mediaid)    REFERENCES media_asset(mediaid)
);

-- ============================================
-- 24. IMAGE_OF_CHOICE_OPTION (replaces IMAGE_OF_OPTION)
-- ============================================
CREATE TABLE image_of_choice_option (
    optionid    BIGINT,
    mediaid     BIGINT,
    CONSTRAINT pk_image_of_choice_option PRIMARY KEY (optionid, mediaid),
    CONSTRAINT fk_image_of_choice_option_option
        FOREIGN KEY (optionid)
            REFERENCES question_choice_option(optionid),
    CONSTRAINT fk_image_of_choice_option_media
        FOREIGN KEY (mediaid)
            REFERENCES image_asset(mediaid)
);

-- ============================================
-- 20. SECTION
-- SECTION(SectionID, Title, Description, Order_Index, Learning_Objective,
--         Section_CourseID)
-- ============================================
CREATE TABLE section (
    sectionid           BIGSERIAL,
    title               VARCHAR(255),
    description         TEXT,
    order_index         INT,
    learning_objective  TEXT,

    -- Theme override for this section (merged on top of course/package theme)
    theme_override      JSONB,

    section_courseid    BIGINT NOT NULL,
    CONSTRAINT pk_section PRIMARY KEY (sectionid),
    CONSTRAINT fk_section_course
        FOREIGN KEY (section_courseid) REFERENCES course(courseid)
);

-- ============================================
-- 21. PAGE
-- PAGE(PageID, Title, Order_Index, Page_Type, SectionID)
-- ============================================
CREATE TABLE page (
    pageid      BIGSERIAL,
    title       VARCHAR(255),
    order_index INT,
    page_type   VARCHAR(50),

    -- Theme override for this page (merged on top of section/theme)
    theme_override JSONB,

    sectionid   BIGINT NOT NULL,
    CONSTRAINT pk_page PRIMARY KEY (pageid),
    CONSTRAINT fk_page_section
        FOREIGN KEY (sectionid) REFERENCES section(sectionid)
);

-- ============================================
-- 22. QUIZ_PAGE
-- QUIZ_PAGE(PageID, Passing_Score, Attempt_Allowed)
-- ============================================
CREATE TABLE quiz_page (
    pageid          BIGINT,
    passing_score   NUMERIC(5,2),
    attempt_allowed INT,
    CONSTRAINT pk_quiz_page PRIMARY KEY (pageid),
    CONSTRAINT fk_quiz_page_page
        FOREIGN KEY (pageid) REFERENCES page(pageid)
);

-- ============================================
-- 23. CONTENT_PAGE
-- CONTENT_PAGE(PageID, Layout_Type)
-- ============================================
CREATE TABLE content_page (
    pageid       BIGINT,
    layout_type  VARCHAR(50),
    CONSTRAINT pk_content_page PRIMARY KEY (pageid),
    CONSTRAINT fk_content_page_page
        FOREIGN KEY (pageid) REFERENCES page(pageid)
);

-- ============================================
-- 24. QUESTION_OF_QUIZ
-- QUESTION_OF_QUIZ(Quiz_QuestionID, Quiz_PageID)
-- ============================================
CREATE TABLE question_of_quiz (
    quiz_questionid  BIGINT,
    quiz_pageid      BIGINT,
    CONSTRAINT pk_question_of_quiz PRIMARY KEY (quiz_questionid, quiz_pageid),
    CONSTRAINT fk_question_of_quiz_question
        FOREIGN KEY (quiz_questionid) REFERENCES question(questionid),
    CONSTRAINT fk_question_of_quiz_quiz_page
        FOREIGN KEY (quiz_pageid)     REFERENCES quiz_page(pageid)
);

-- ============================================
-- 25. CONTENT_BLOCK
-- CONTENT_BLOCK(BlockID, Order_Index, Text_Html, Content_PageID)
-- ============================================
CREATE TABLE content_block (
    blockid         BIGSERIAL,
    order_index     INT,
    text_html       TEXT,
    content_pageid  BIGINT NOT NULL,
    CONSTRAINT pk_content_block PRIMARY KEY (blockid),
    CONSTRAINT fk_content_block_content_page
        FOREIGN KEY (content_pageid) REFERENCES content_page(pageid)
);

-- ============================================
-- 26. NOTIFICATION
-- Lưu trữ các thông báo gửi đến người dùng
-- ============================================
CREATE TABLE notification (
    notificationid  BIGSERIAL,
    
    -- Người nhận thông báo
    userid          BIGINT NOT NULL,
    
    -- Nội dung thông báo
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    
    -- Loại thông báo để frontend hiển thị icon/màu sắc tương ứng 
    -- (VD: 'SYSTEM', 'COURSE_INVITE', 'ORG_UPDATE', 'WARNING')
    type            VARCHAR(50) NOT NULL,
    
    -- Trạng thái đã đọc/chưa đọc
    is_read         BOOLEAN DEFAULT FALSE,
    
    -- [Tùy chọn] Lưu ID và Loại của Entity liên quan để khi user click vào thông báo sẽ chuyển hướng (redirect) đúng chỗ
    -- VD: reference_id = 15, reference_type = 'COURSE' -> Chuyển hướng đến khóa học có ID 15
    reference_id    BIGINT,
    reference_type  VARCHAR(50),
    
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT pk_notification PRIMARY KEY (notificationid),
    CONSTRAINT fk_notification_user
        FOREIGN KEY (userid) REFERENCES users(userid) ON DELETE CASCADE
);

-- Tạo Index để tăng tốc độ truy vấn do sau này hệ thống sẽ liên tục query: 
-- "Lấy danh sách thông báo của user A" hoặc "Đếm số thông báo CHƯA ĐỌC của user A"
CREATE INDEX idx_notification_userid ON notification(userid);
CREATE INDEX idx_notification_userid_is_read ON notification(userid, is_read);
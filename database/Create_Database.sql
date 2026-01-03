-- ============================================
-- 2. USERS
-- USER(UserID, Fname, Minit, Name, Email, Password_Hash, Avatar_Url,
--      Is_Active, Last_Login_At, Created_At, Updated_At)
-- ============================================
CREATE TABLE users (
    userid          BIGSERIAL,
    fname           VARCHAR(100),
    minit           VARCHAR(10),
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
    status              VARCHAR(50),
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
    extra_config     JSONB,
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
    zip_file_path     TEXT,
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
-- 16. QUESTION
-- QUESTION(QuestionID, Title, Instruction, Prompt_Html, Extra_Config,
--          Question_Type)
-- ============================================
CREATE TABLE question (
    questionid     BIGSERIAL,
    title          VARCHAR(255),
    instruction    TEXT,
    prompt_html    TEXT,
    extra_config   JSONB,
    question_type  VARCHAR(50),
    CONSTRAINT pk_question PRIMARY KEY (questionid)
);

-- ============================================
-- 17. QUESTION_OPTION
-- QUESTION_OPTION(OptionID, QuestionID, Is_Correst, Content_Html)
-- ============================================
CREATE TABLE question_option (
    optionid      BIGSERIAL,
    questionid    BIGINT,
    is_correst    BOOLEAN,
    content_html  TEXT,
    CONSTRAINT pk_question_option PRIMARY KEY (optionid, questionid),
    CONSTRAINT fk_question_option_question
        FOREIGN KEY (questionid) REFERENCES question(questionid)
);

-- ============================================
-- 18. MEDIA_OF_QUESTION
-- MEDIA_OF_QUESTION(QuestionID, MediaID)
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
-- 19. IMAGE_OF_OPTION
-- IMAGE_OF_OPTION(OptionID, QuestionID, MediaID)
-- ============================================
CREATE TABLE image_of_option (
    optionid    BIGINT,
    questionid  BIGINT,
    mediaid     BIGINT,
    CONSTRAINT pk_image_of_option PRIMARY KEY (optionid, questionid),
    CONSTRAINT fk_image_of_option_question_option
        FOREIGN KEY (optionid, questionid)
            REFERENCES question_option(optionid, questionid),
    CONSTRAINT fk_image_of_option_media
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

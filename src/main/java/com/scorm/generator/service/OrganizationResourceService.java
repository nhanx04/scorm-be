package com.scorm.generator.service;

import com.scorm.generator.dto.LibraryDetailResponse;
import com.scorm.generator.dto.OrganizationActivityResponse;
import com.scorm.generator.dto.OrganizationFolderAssetsResponse;
import com.scorm.generator.dto.ResourceResponse;
import com.scorm.generator.dto.ShareResourceRequest;
import com.scorm.generator.entity.Course;
import com.scorm.generator.entity.MediaAsset;
import com.scorm.generator.entity.MyLibrary;
import com.scorm.generator.entity.Organization;
import com.scorm.generator.entity.OrganizationActivity;
import com.scorm.generator.entity.OrganizationActivityAction;
import com.scorm.generator.entity.OrganizationResource;
import com.scorm.generator.entity.OrganizationResourceType;
import com.scorm.generator.entity.User;
import com.scorm.generator.exception.AppException;
import com.scorm.generator.repository.CourseRepository;
import com.scorm.generator.repository.MediaAssetRepository;
import com.scorm.generator.repository.MyLibraryRepository;
import com.scorm.generator.repository.OrganizationActivityRepository;
import com.scorm.generator.repository.OrganizationRepository;
import com.scorm.generator.repository.OrganizationResourceRepository;
import com.scorm.generator.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationResourceService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationResourceRepository organizationResourceRepository;
    private final OrganizationActivityRepository organizationActivityRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final CourseRepository courseRepository;
    private final MyLibraryRepository myLibraryRepository;

    public OrganizationResourceService(
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            OrganizationResourceRepository organizationResourceRepository,
            OrganizationActivityRepository organizationActivityRepository,
            MediaAssetRepository mediaAssetRepository,
            CourseRepository courseRepository,
            MyLibraryRepository myLibraryRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.organizationResourceRepository = organizationResourceRepository;
        this.organizationActivityRepository = organizationActivityRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.courseRepository = courseRepository;
        this.myLibraryRepository = myLibraryRepository;
    }

    @Transactional
    public ResourceResponse shareResource(Long orgId, ShareResourceRequest request, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Organization organization = requireOrganization(orgId);
        assertMember(orgId, currentUser.getUserId());

        int providedIds = countNotNull(request.getMediaAssetId(), request.getCourseId(), request.getFolderId());
        if (providedIds != 1) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Exactly one of mediaAssetId, courseId, folderId must be provided");
        }

        OrganizationResource resource = OrganizationResource.builder()
                .organization(organization)
                .sharedBy(currentUser)
                .build();

        if (request.getMediaAssetId() != null) {
            MediaAsset mediaAsset = mediaAssetRepository.findById(request.getMediaAssetId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "MediaAsset not found"));
            resource.setMediaAsset(mediaAsset);
            resource.setType(OrganizationResourceType.MEDIA);
        } else if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Course not found"));
            resource.setCourse(course);
            resource.setType(OrganizationResourceType.COURSE);
        } else {
            MyLibrary folder = myLibraryRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Folder not found"));
            resource.setFolder(folder);
            resource.setType(OrganizationResourceType.FOLDER);
        }

        if (request.getType() != null && request.getType() != resource.getType()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "type does not match provided resource id");
        }

        OrganizationResource saved = organizationResourceRepository.save(resource);
        logActivity(organization, currentUser, actionForType(saved.getType()), saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getResources(Long orgId, OrganizationResourceType type,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        requireOrganization(orgId);
        assertMember(orgId, currentUser.getUserId());

        List<OrganizationResource> resources = (type == null)
                ? organizationResourceRepository.findByOrganization_OrgIdOrderByCreatedAtDesc(orgId)
                : organizationResourceRepository.findByOrganization_OrgIdAndTypeOrderByCreatedAtDesc(orgId, type);

        return resources.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteResource(Long orgId, Long resourceId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Organization organization = requireOrganization(orgId);
        assertMember(orgId, currentUser.getUserId());

        OrganizationResource resource = organizationResourceRepository.findByIdAndOrganization_OrgId(resourceId, orgId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Organization resource not found"));

        organizationResourceRepository.delete(resource);
        logActivity(organization, currentUser, OrganizationActivityAction.DELETE_RESOURCE, resourceId);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceDetail(Long orgId, Long resourceId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        requireOrganization(orgId);
        assertMember(orgId, currentUser.getUserId());

        OrganizationResource resource = organizationResourceRepository.findByIdAndOrganization_OrgId(resourceId, orgId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Organization resource not found"));

        return toResponse(resource);
    }

    @Transactional(readOnly = true)
    public OrganizationFolderAssetsResponse getSharedFolderAssets(Long orgId, Long folderId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        requireOrganization(orgId);
        assertMember(orgId, currentUser.getUserId());

        OrganizationResource folderResource = organizationResourceRepository
                .findByOrganization_OrgIdAndTypeAndFolder_LibraryId(orgId, OrganizationResourceType.FOLDER, folderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Folder is not shared in this organization"));

        MyLibrary folder = folderResource.getFolder();
        List<LibraryDetailResponse.MediaAssetItem> items = mediaAssetRepository
                .findByLibrary_LibraryIdOrderByUploadedAtDesc(folderId)
                .stream()
                .map(a -> LibraryDetailResponse.MediaAssetItem.builder()
                        .mediaId(a.getMediaId())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .originalFileName(a.getOriginalFileName())
                        .mediaType(a.getMediaType())
                        .uploadedAt(a.getUploadedAt())
                        .updatedAt(a.getUpdatedAt())
                        .metadata(a.getMetadata())
                        .build())
                .toList();

        return OrganizationFolderAssetsResponse.builder()
                .orgId(orgId)
                .resourceId(folderResource.getId())
                .folderId(folderId)
                .folderName(folder == null ? null : folder.getLibraryName())
                .items(items)
                .build();
    }

    private ResourceResponse toResponse(OrganizationResource resource) {
        String name = null;
        String thumbnail = null;
        String instructor = null;
        Integer folderItemCount = null;

        if (resource.getMediaAsset() != null) {
            name = resource.getMediaAsset().getTitle();
            thumbnail = resource.getMediaAsset().getOriginalFileName();
        } else if (resource.getCourse() != null) {
            name = resource.getCourse().getTitle();
            User courseOwner = resource.getCourse().getUser();
            instructor = courseOwner == null ? null : (courseOwner.getFname() + " " + courseOwner.getLname()).trim();
        } else if (resource.getFolder() != null) {
            name = resource.getFolder().getLibraryName();
            folderItemCount = Math
                    .toIntExact(mediaAssetRepository.countByLibrary_LibraryId(resource.getFolder().getLibraryId()));
        }

        User sharedBy = resource.getSharedBy();
        String sharedByName = sharedBy == null ? null : (sharedBy.getFname() + " " + sharedBy.getLname()).trim();

        return ResourceResponse.builder()
                .id(resource.getId())
                .type(resource.getType())
                .name(name)
                .thumbnail(thumbnail)
                .instructor(instructor)
                .folderItemCount(folderItemCount)
                .mediaAssetId(resource.getMediaAsset() == null ? null : resource.getMediaAsset().getMediaId())
                .courseId(resource.getCourse() == null ? null : resource.getCourse().getCourseId())
                .folderId(resource.getFolder() == null ? null : resource.getFolder().getLibraryId())
                .sharedBy(sharedBy == null ? null : sharedBy.getUserId())
                .sharedByName(sharedByName)
                .createdAt(resource.getCreatedAt())
                .build();
    }

    private OrganizationActivityResponse toActivityResponse(OrganizationActivity activity) {
        User user = activity.getUser();
        String userName = user == null ? null : (user.getFname() + " " + user.getLname()).trim();

        return OrganizationActivityResponse.builder()
                .id(activity.getId())
                .action(activity.getAction())
                .targetId(activity.getTargetId())
                .userId(user == null ? null : user.getUserId())
                .userName(userName)
                .createdAt(activity.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrganizationActivityResponse> getActivities(Long orgId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        requireOrganization(orgId);
        assertMember(orgId, currentUser.getUserId());

        return organizationActivityRepository.findByOrganization_OrgIdOrderByCreatedAtDesc(orgId)
                .stream()
                .map(this::toActivityResponse)
                .toList();
    }

    private void assertMember(Long orgId, Long userId) {
        membershipRepository.findByOrganization_OrgIdAndUser_UserId(orgId, userId)
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()) || "INVITED".equalsIgnoreCase(m.getStatus()))
                .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "You are not a member of this organization"));
    }

    private Organization requireOrganization(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Organization not found"));
    }

    private void logActivity(Organization organization, User user, OrganizationActivityAction action, Long targetId) {
        OrganizationActivity activity = OrganizationActivity.builder()
                .organization(organization)
                .user(user)
                .action(action)
                .targetId(targetId)
                .build();
        organizationActivityRepository.save(activity);
    }

    private int countNotNull(Object... values) {
        int count = 0;
        for (Object value : values) {
            if (value != null) {
                count++;
            }
        }
        return count;
    }

    private OrganizationActivityAction actionForType(OrganizationResourceType type) {
        return switch (type) {
            case MEDIA -> OrganizationActivityAction.SHARE_MEDIA;
            case COURSE -> OrganizationActivityAction.SHARE_COURSE;
            case FOLDER -> OrganizationActivityAction.SHARE_FOLDER;
        };
    }
}

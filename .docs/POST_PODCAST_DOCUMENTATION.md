# Post / Podcast System — Full Documentation

> Reference guide for reimplementing the post/Podcast functionality from the Skateboard Podcast Magazine app in another application.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Data Models](#data-models)
4. [State Management](#state-management)
5. [Use Cases & User Flows](#use-cases--user-flows)
6. [Screen Reference](#screen-reference)
7. [Component Reference](#component-reference)
8. [Navigation](#navigation)
9. [Storage & Persistence](#storage--persistence)
10. [Settings](#settings)
11. [Theming](#theming)
12. [Dependencies](#dependencies)
13. [Reimplementation Checklist](#reimplementation-checklist)

---

## Overview


**Core capabilities:**
- Create, read, update, and delete posts
- Rich content composed of multiple typed blocks (text, image, video, quote, embed, gallery, link, Spotify)
- Infinite-scroll Podcast showing only published posts
- Configurable pagination (5–50 posts per page)
- Platform-aware rendering (iframe embeds on web, native fallbacks on mobile)
 
## Data Models

### `PostStatus`

```typescript
type PostStatus = 'draft' | 'scheduled' | 'published';
```

### `Post`

```typescript
type Post = {
  id: string;           // Unique ID — generated as Date.now().toString()
  slug: string;         // URL-safe string auto-generated from the title
  title: string;        // Post headline
  status: PostStatus;   // 'draft' | 'scheduled' | 'published'
  publishAt: string | null;  // ISO 8601 timestamp; used when status is 'scheduled'
  coverUrl: string;     // Cover image — remote URL or base64 data URI from local upload
  blocks: Block[];      // Ordered array of content blocks (see below)
  createdAt: string;    // ISO 8601 creation timestamp
  updatedAt: string;    // ISO 8601 last-modified timestamp
  createdBy: string;    // Creator identifier (e.g. 'user')
};
```

### Block Types

Every block has a `type` discriminant and a `data` payload.

```typescript
type BlockType =
  | 'text'
  | 'image'
  | 'video'
  | 'quote'
  | 'embed'
  | 'gallery'
  | 'link'
  | 'spotify';
```

#### `TextBlock`

```typescript
type TextBlock = {
  type: 'text';
  data: {
    html: string;   // HTML string produced by the rich-text editor
  };
};
```

#### `ImageBlock`

```typescript
type ImageBlock = {
  type: 'image';
  data: {
    url: string;         // Remote URL or base64 data URI
    caption?: string;    // Optional caption displayed below the image
    isUpload?: boolean;  // True when image was picked from device gallery
  };
};
```

#### `VideoBlock`

```typescript
type VideoBlock = {
  type: 'video';
  data: {
    url: string;       // Direct video URL (mp4, etc.)
    poster?: string;   // Optional thumbnail URL
  };
};
```

#### `QuoteBlock`

```typescript
type QuoteBlock = {
  type: 'quote';
  data: {
    text: string;      // Quote body
    author?: string;   // Optional attribution
  };
};
```

#### `EmbedBlock` (YouTube / Vimeo)

```typescript
type EmbedBlock = {
  type: 'embed';
  data: {
    platform: 'youtube' | 'vimeo';
    id: string;   // Video ID extracted from the URL
  };
};
```

#### `SpotifyBlock`

```typescript
type SpotifyBlock = {
  type: 'spotify';
  data: {
    url: string;                                                   // Full Spotify URL
    spotifyType: 'track' | 'album' | 'playlist' | 'episode' | 'show';
    spotifyId: string;   // ID extracted from the URL
  };
};
```

Spotify URLs are parsed with this regex map:

| Type | Pattern |
|------|---------|
| track | `open.spotify.com/track/` |
| album | `open.spotify.com/album/` |
| playlist | `open.spotify.com/playlist/` |
| episode | `open.spotify.com/episode/` |
| show | `open.spotify.com/show/` |

#### `GalleryBlock`

```typescript
type GalleryBlock = {
  type: 'gallery';
  data: {
    urls: string[];           // Array of image URLs or base64 data URIs
    isUpload?: boolean[];     // Parallel array; true = local upload per index
  };
};
```

#### `LinkBlock`

```typescript
type LinkBlock = {
  type: 'link';
  data: {
    url: string;
    title?: string;
    description?: string;
  };
};
```

---

## State Management
 
```typescript
// Shape of the context value
type PostsContextType = {
  posts: Post[];
  isLoading: boolean;

  addPost: (post: Omit<Post, 'id' | 'createdAt' | 'updatedAt'>) => Promise<void>;
  updatePost: (id: string, updates: Partial<Post>) => Promise<void>;
  deletePost: (id: string) => Promise<void>;
  getPostBySlug: (slug: string) => Post | undefined;
  getPublishedPosts: () => Post[];
  resetPosts: () => Promise<void>;
};
```

**Behaviors:**
- `addPost` prepends the new post (`[newPost, ...posts]`) so the Podcast shows newest first.
- `addPost` auto-sets `id = Date.now().toString()`, `createdAt`, and `updatedAt`.
- `updatePost` updates `updatedAt` automatically.
- `getPublishedPosts` filters `posts.filter(p => p.status === 'published')`.


### `SettingsContext`
 
```typescript
type Settings = {
  postsPerPage: number;   // Default: 10  |  Range: 5–50
};

type SettingsContextType = {
  settings: Settings;
  isLoading: boolean;
  updatePostsPerPage: (n: number) => Promise<void>;
};
```

---

## Use Cases & User Flows

### UC-1: Browse Podcast

1. User opens the app — Podcast tab is active.
2. `getPublishedPosts()` returns all posts with `status === 'published'`.
3. Podcast shows a paginated list. Default page size is `settings.postsPerPage` (10).
4. Scrolling to 50 % from the bottom triggers `onEndReached`, which increments the page counter.
5. More posts are sliced from the in-memory array and appended to the visible list.
6. Footer shows "Showing X of Y posts" while more remain; "All X posts loaded" when done.

### UC-2: View Post Detail

1. User taps a post card in the Podcast.
2. Navigation pushes `/post/[slug]`.
3. Detail screen fetches the post via `getPostBySlug(slug)`.
4. Renders: cover image → title → date → divider → blocks (via `BlockRenderer`).
5. Edit and Delete buttons appear in the header.

### UC-3: Create a Post

1. User taps the FAB (➕) or "Write your first post" button.
2. Navigation pushes `/create-post` (modal).
3. User fills in:
   - **Title** (required)
   - **Cover image** — URL or device gallery pick (required)
   - **Content blocks** — one or more, added via type buttons
4. On submit: validation runs → `addPost()` called → modal dismissed → Podcast refreshes.

**Validation rules:**
- Title must not be empty.
- Cover URL must not be empty.
- At least one block must exist.

**Slug generation:**

```typescript
function generateSlug(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}
```

### UC-4: Edit a Post

1. From the detail screen, user taps the Edit (pencil) button.
2. Navigation pushes `/edit-post/[id]`.
3. Screen pre-populates all fields from the existing post.
4. On submit: `updatePost(id, updates)` called → modal dismissed.

### UC-5: Delete a Post

1. From the detail screen, user taps the Delete (trash) button.
2. A confirmation `Alert` is shown.
3. On confirm: `deletePost(id)` → navigate back to Podcast.

### UC-6: Scheduled Publishing

1. During post creation, user sets `status = 'scheduled'` and supplies a `publishAt` ISO timestamp.
2. The post is stored with `status: 'scheduled'`.
3. **Note:** The app has no background scheduler. Status promotion from `scheduled` → `published` must be triggered manually or via an external job when integrating a backend.

---

## Screen Reference

### Podcast Screen 

| Element | Description |
|---------|-------------|
| Header | Title + "New Post" button (top-right) |
| FlatList | Paginated post cards; `onEndReachedThreshold={0.5}` |
| Post card | Cover image (200 px height) + title + formatted date |
| Empty state | "No posts yet" message + "Write your first post" button |
| FAB | Floating ➕ button; navigates to `/create-post` |
| Footer | Pagination status label; spinner when loading more |

### Create Post Screen — 

| Field | Type | Required |
|-------|------|----------|
| Title | TextInput | Yes |
| Cover image source | Toggle (URL / Upload) | — |
| Cover URL | TextInput | Yes (if URL mode) |
| Cover image picker | expo-image-picker | Yes (if Upload mode) |
| Content blocks | Dynamic list | Min 1 |

**Block add buttons (in order):** Text · Image · Quote · Video · YouTube · Gallery · Link · Spotify

### Edit Post Screen —  

Same form as Create, pre-populated with existing post data. Calls `updatePost` instead of `addPost`.

### Post Detail Screen 

Renders the full post:
1. Cover image (300 px height)
2. Title (`h1` style)
3. Creation date
4. Horizontal divider
5. All blocks via `<BlockRenderer block={block} />`

---

## Component Reference

### `BlockRenderer`
 

```typescript
<BlockRenderer block={block} />
```

Renders any `Block` union type. Behavior per type:

| Block type | Web | Mobile |
|-----------|-----|--------|
| `text` | Strips HTML tags, renders plain text | Same |
| `image` | `<Image>` (240 px h) + optional caption | Same |
| `video` | `<video>` element with controls | Text placeholder |
| `quote` | Styled blockquote with accent-color left border | Same |
| `embed` (YouTube) | `<iframe>` | Text placeholder "YouTube: {id}" |
| `embed` (Vimeo) | `<iframe>` | Text placeholder "Vimeo: {id}" |
| `gallery` | 2-column flex grid, 160 px images | Same |
| `link` | Pressable card — opens URL | Same |
| `spotify` | `<iframe>` embed | `SpotifyMiniCard` component |

**`SpotifyMiniCard` (mobile):**
- Fetches metadata from `https://open.spotify.com/oembed?url={url}`.
- Displays: 64×64 artwork · title · artist / author · "Play" button.
- Tap opens the Spotify app via `Linking.openURL()`.

### `RichTextEditor`

**File:** `expo/components/RichTextEditor.tsx`

```typescript
<RichTextEditor
  value={htmlString}
  onChange={(html: string) => void}
/>
```

Produces and consumes HTML strings. Toolbar supports: Bold · Italic · Underline · Bullet list · Numbered list.

## Settings  (Only admin)

The Settings tab  exposes:

| Setting | Type | Default | Range |
|---------|------|---------|-------|
| Posts per page | Slider / input | `10` | 5–50 |

Changing this setting takes effect immediately on the next Podcast render. 
 

## Reimplementation Checklist

Use this checklist when porting the system to a new project.

### Data layer
- [ ] Define `Post`, `Block`, and all block-type interfaces
- [ ] Implement `generateSlug(title)` utility
- [ ] Implement `extractSpotifyInfo(url)` utility
- [ ] Choose a persistence layer (AsyncStorage for local, Supabase/Firebase for remote)
- [ ] Implement CRUD: `addPost`, `updatePost`, `deletePost`, `getPostBySlug`, `getPublishedPosts`

### Podcast screen
- [ ] FlatList (or equivalent) consuming `getPublishedPosts()`
- [ ] Infinite scroll: page counter + `onEndReached` + `slice(0, page * pageSize)`
- [ ] Post card: cover image + title + date
- [ ] Empty state with CTA
- [ ] FAB / header button linking to create screen

### Create / Edit screen
- [ ] Title field
- [ ] Cover image field (URL input + optional gallery picker)
- [ ] Block list with add / remove / reorder
- [ ] Per-block editors (text, image, quote, video, embed, gallery, link, Spotify)
- [ ] Validation before submit
- [ ] Call `addPost` or `updatePost` on submit

### Detail screen
- [ ] Load post by slug
- [ ] Render cover, title, date, blocks
- [ ] Edit and Delete actions in header

### Block renderer
- [ ] One renderer per block type
- [ ] Platform detection for video and embed blocks
- [ ] Spotify oEmbed fetch for mobile fallback
- [ ] `Linking.openURL` for link blocks and Spotify taps

### Settings
- [ ] `postsPerPage` setting stored and read by the Podcast

### Navigation
- [ ] `/` or equivalent for Podcast
- [ ] `/create-post` modal
- [ ] `/edit-post/[id]` modal
- [ ] `/post/[slug]` detail push

### Optional upgrades (not in current implementation)
- [ ] Backend API replacing AsyncStorage
- [ ] Image CDN replacing base64 in storage
- [ ] Background scheduler for `scheduled` posts
- [ ] Likes, comments, bookmarks
- [ ] Share sheet integration
- [ ] Search / filter by tag or category
- [ ] Offline queue for failed API writes

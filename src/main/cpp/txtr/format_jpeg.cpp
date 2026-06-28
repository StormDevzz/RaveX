#include "txtr.h"
#include "include/txtr_utils.h"
#include <cstring>
#include <vector>

#if defined(TXTR_HAVE_LIBJPEG)
#include <jpeglib.h>
#endif

namespace txtr {

#if defined(TXTR_HAVE_LIBJPEG)
struct JpegMemSrc {
    jpeg_source_mgr pub;
    const uint8_t* data;
    size_t size;
    size_t pos;
};

static void jpegMemInitSource(j_decompress_ptr) {}
static boolean jpegMemFillInputBuffer(j_decompress_ptr cinfo) {
    JpegMemSrc* src = reinterpret_cast<JpegMemSrc*>(cinfo->src);
    if (src->pos >= src->size) {
        src->pub.bytes_in_buffer = 0;
        return TRUE;
    }
    src->pub.next_input_byte = src->data + src->pos;
    src->pub.bytes_in_buffer = src->size - src->pos;
    src->pos = src->size;
    return TRUE;
}
static void jpegMemSkipInputData(j_decompress_ptr cinfo, long num_bytes) {
    JpegMemSrc* src = reinterpret_cast<JpegMemSrc*>(cinfo->src);
    if (num_bytes > 0) {
        src->pub.next_input_byte += num_bytes;
        src->pub.bytes_in_buffer -= num_bytes;
    }
}
static void jpegMemTermSource(j_decompress_ptr) {}

static void jpegSetMemSrc(j_decompress_ptr cinfo, const uint8_t* data, size_t size) {
    JpegMemSrc* src = reinterpret_cast<JpegMemSrc*>((*cinfo->mem->alloc_small)(
        reinterpret_cast<j_common_ptr>(cinfo), JPOOL_PERMANENT, sizeof(JpegMemSrc)));
    src->pub.init_source = jpegMemInitSource;
    src->pub.fill_input_buffer = jpegMemFillInputBuffer;
    src->pub.skip_input_data = jpegMemSkipInputData;
    src->pub.resync_to_restart = jpeg_resync_to_restart;
    src->pub.term_source = jpegMemTermSource;
    src->pub.bytes_in_buffer = size;
    src->pub.next_input_byte = data;
    src->data = data;
    src->size = size;
    src->pos = 0;
    cinfo->src = &src->pub;
}

struct JpegMemDst {
    jpeg_destination_mgr pub;
    std::vector<uint8_t>* out;
};

static void jpegMemInitDestination(j_compress_ptr) {}
static boolean jpegMemEmptyOutputBuffer(j_compress_ptr cinfo) {
    JpegMemDst* dst = reinterpret_cast<JpegMemDst*>(cinfo->dest);
    size_t oldSize = dst->out->size();
    dst->out->resize(oldSize + 4096);
    dst->pub.next_output_byte = dst->out->data() + oldSize;
    dst->pub.free_in_buffer = dst->out->size() - oldSize;
    return TRUE;
}
static void jpegMemTermDestination(j_compress_ptr cinfo) {
    JpegMemDst* dst = reinterpret_cast<JpegMemDst*>(cinfo->dest);
    dst->out->resize(dst->out->size() - dst->pub.free_in_buffer);
}

static void jpegSetMemDst(j_compress_ptr cinfo, std::vector<uint8_t>& out) {
    if (cinfo->dest == nullptr) {
        JpegMemDst* dst = reinterpret_cast<JpegMemDst*>((*cinfo->mem->alloc_small)(
            reinterpret_cast<j_common_ptr>(cinfo), JPOOL_PERMANENT, sizeof(JpegMemDst)));
        dst->pub.init_destination = jpegMemInitDestination;
        dst->pub.empty_output_buffer = jpegMemEmptyOutputBuffer;
        dst->pub.term_destination = jpegMemTermDestination;
        cinfo->dest = &dst->pub;
    }
    JpegMemDst* dst = reinterpret_cast<JpegMemDst*>(cinfo->dest);
    dst->out = &out;
    out.resize(4096);
    dst->pub.next_output_byte = out.data();
    dst->pub.free_in_buffer = out.size();
}

TextureData loadJpeg(const uint8_t* data, size_t size) {
    TextureData tex;
    jpeg_decompress_struct cinfo;
    jpeg_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_decompress(&cinfo);
    jpegSetMemSrc(&cinfo, data, size);
    jpeg_read_header(&cinfo, TRUE);
    cinfo.out_color_space = JCS_EXT_RGBA;
    tex.width = cinfo.image_width;
    tex.height = cinfo.image_height;
    tex.channels = 4;
    tex.pixels.resize(static_cast<size_t>(tex.width) * tex.height * 4);
    jpeg_start_decompress(&cinfo);
    while (cinfo.output_scanline < cinfo.output_height) {
        JSAMPROW row = tex.pixels.data() + cinfo.output_scanline * tex.pitch();
        jpeg_read_scanlines(&cinfo, &row, 1);
    }
    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    return tex;
}

ErrorCode saveJpeg(const TextureData& tex, std::vector<uint8_t>& out, int quality) {
    if (tex.channels < 3) return ErrorCode::FormatMismatch;
    jpeg_compress_struct cinfo;
    jpeg_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_compress(&cinfo);
    jpegSetMemDst(&cinfo, out);
    cinfo.image_width = tex.width;
    cinfo.image_height = tex.height;
    cinfo.input_components = 3;
    cinfo.in_color_space = JCS_RGB;
    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, quality, TRUE);
    jpeg_start_compress(&cinfo, TRUE);
    std::vector<uint8_t> rowBuf(static_cast<size_t>(tex.width) * 3);
    while (cinfo.next_scanline < cinfo.image_height) {
        for (int x = 0; x < tex.width; ++x) {
            rowBuf[x * 3 + 0] = tex.pixels[(cinfo.next_scanline * tex.pitch()) + x * 4 + 0];
            rowBuf[x * 3 + 1] = tex.pixels[(cinfo.next_scanline * tex.pitch()) + x * 4 + 1];
            rowBuf[x * 3 + 2] = tex.pixels[(cinfo.next_scanline * tex.pitch()) + x * 4 + 2];
        }
        JSAMPROW row = rowBuf.data();
        jpeg_write_scanlines(&cinfo, &row, 1);
    }
    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);
    return ErrorCode::Success;
}
#else
TextureData loadJpeg(const uint8_t*, size_t) {
    throw Error(ErrorCode::NotImplemented, "JPEG requires libjpeg");
}

ErrorCode saveJpeg(const TextureData&, std::vector<uint8_t>&, int) {
    throw Error(ErrorCode::NotImplemented, "JPEG requires libjpeg");
}
#endif

} // namespace txtr

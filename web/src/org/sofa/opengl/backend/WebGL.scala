package org.sofa.opengl.backend

import scala.scalajs.js
import org.scalajs.dom.{HTMLElement, HTMLCanvasElement}


trait WebGLRenderingContext extends js.Object {
	val DEPTH_BUFFER_BIT              :js.Number = ???
    val STENCIL_BUFFER_BIT            :js.Number = ???
    val COLOR_BUFFER_BIT              :js.Number = ???

    val POINTS                        :js.Number = ???
    val LINES                         :js.Number = ???
    val LINE_LOOP                     :js.Number = ???
    val LINE_STRIP                    :js.Number = ???
    val TRIANGLES                     :js.Number = ???
    val TRIANGLE_STRIP                :js.Number = ???
    val TRIANGLE_FAN                  :js.Number = ???
    
    /* AlphaFunction (not supported in ES20) */
    /*      NEVER */
    /*      LESS */
    /*      EQUAL */
    /*      LEQUAL */
    /*      GREATER */
    /*      NOTEQUAL */
    /*      GEQUAL */
    /*      ALWAYS */
    
    /* BlendingFactorDest */
    val ZERO                          :js.Number = ???
    val ONE                           :js.Number = ???
    val SRC_COLOR                     :js.Number = ???
    val ONE_MINUS_SRC_COLOR           :js.Number = ???
    val SRC_ALPHA                     :js.Number = ???
    val ONE_MINUS_SRC_ALPHA           :js.Number = ???
    val DST_ALPHA                     :js.Number = ???
    val ONE_MINUS_DST_ALPHA           :js.Number = ???
    
    /* BlendingFactorSrc */
    /*      ZERO */
    /*      ONE */
    val DST_COLOR                     :js.Number = ???
    val ONE_MINUS_DST_COLOR           :js.Number = ???
    val SRC_ALPHA_SATURATE            :js.Number = ???
    /*      SRC_ALPHA */
    /*      ONE_MINUS_SRC_ALPHA */
    /*      DST_ALPHA */
    /*      ONE_MINUS_DST_ALPHA */
    
    /* BlendEquationSeparate */
    val FUNC_ADD                      :js.Number = ???
    val BLEND_EQUATION                :js.Number = ???
    val BLEND_EQUATION_RGB            :js.Number = ???   /* same as BLEND_EQUATION */
    val BLEND_EQUATION_ALPHA          :js.Number = ???
    
    /* BlendSubtract */
    val FUNC_SUBTRACT                 :js.Number = ???
    val FUNC_REVERSE_SUBTRACT         :js.Number = ???
    
    /* Separate Blend Functions */
    val BLEND_DST_RGB                 :js.Number = ???
    val BLEND_SRC_RGB                 :js.Number = ???
    val BLEND_DST_ALPHA               :js.Number = ???
    val BLEND_SRC_ALPHA               :js.Number = ???
    val CONSTANT_COLOR                :js.Number = ???
    val ONE_MINUS_CONSTANT_COLOR      :js.Number = ???
    val CONSTANT_ALPHA                :js.Number = ???
    val ONE_MINUS_CONSTANT_ALPHA      :js.Number = ???
    val BLEND_COLOR                   :js.Number = ???
    
    /* Buffer Objects */
    val ARRAY_BUFFER                  :js.Number = ???
    val ELEMENT_ARRAY_BUFFER          :js.Number = ???
    val ARRAY_BUFFER_BINDING          :js.Number = ???
    val ELEMENT_ARRAY_BUFFER_BINDING  :js.Number = ???
    
    val STREAM_DRAW                   :js.Number = ???
    val STATIC_DRAW                   :js.Number = ???
    val DYNAMIC_DRAW                  :js.Number = ???
    
    val BUFFER_SIZE                   :js.Number = ???
    val BUFFER_USAGE                  :js.Number = ???
    
    val CURRENT_VERTEX_ATTRIB         :js.Number = ???
    
    /* CullFaceMode */
    val FRONT                         :js.Number = ???
    val BACK                          :js.Number = ???
    val FRONT_AND_BACK                :js.Number = ???
    
    /* DepthFunction */
    /*      NEVER */
    /*      LESS */
    /*      EQUAL */
    /*      LEQUAL */
    /*      GREATER */
    /*      NOTEQUAL */
    /*      GEQUAL */
    /*      ALWAYS */
    
    /* EnableCap */
    /* TEXTURE_2D */
    val CULL_FACE                     :js.Number = ???
    val BLEND                         :js.Number = ???
    val DITHER                        :js.Number = ???
    val STENCIL_TEST                  :js.Number = ???
    val DEPTH_TEST                    :js.Number = ???
    val SCISSOR_TEST                  :js.Number = ???
    val POLYGON_OFFSET_FILL           :js.Number = ???
    val SAMPLE_ALPHA_TO_COVERAGE      :js.Number = ???
    val SAMPLE_COVERAGE               :js.Number = ???
    
    /* ErrorCode */
    val NO_ERROR                      :js.Number = ???
    val INVALID_ENUM                  :js.Number = ???
    val INVALID_VALUE                 :js.Number = ???
    val INVALID_OPERATION             :js.Number = ???
    val OUT_OF_MEMORY                 :js.Number = ???
    
    /* FrontFaceDirection */
    val CW                            :js.Number = ???
    val CCW                           :js.Number = ???
    
    /* GetPName */
    val LINE_WIDTH                    :js.Number = ???
    val ALIASED_POINT_SIZE_RANGE      :js.Number = ???
    val ALIASED_LINE_WIDTH_RANGE      :js.Number = ???
    val CULL_FACE_MODE                :js.Number = ???
    val FRONT_FACE                    :js.Number = ???
    val DEPTH_RANGE                   :js.Number = ???
    val DEPTH_WRITEMASK               :js.Number = ???
    val DEPTH_CLEAR_VALUE             :js.Number = ???
    val DEPTH_FUNC                    :js.Number = ???
    val STENCIL_CLEAR_VALUE           :js.Number = ???
    val STENCIL_FUNC                  :js.Number = ???
    val STENCIL_FAIL                  :js.Number = ???
    val STENCIL_PASS_DEPTH_FAIL       :js.Number = ???
    val STENCIL_PASS_DEPTH_PASS       :js.Number = ???
    val STENCIL_REF                   :js.Number = ???
    val STENCIL_VALUE_MASK            :js.Number = ???
    val STENCIL_WRITEMASK             :js.Number = ???
    val STENCIL_BACK_FUNC             :js.Number = ???
    val STENCIL_BACK_FAIL             :js.Number = ???
    val STENCIL_BACK_PASS_DEPTH_FAIL  :js.Number = ???
    val STENCIL_BACK_PASS_DEPTH_PASS  :js.Number = ???
    val STENCIL_BACK_REF              :js.Number = ???
    val STENCIL_BACK_VALUE_MASK       :js.Number = ???
    val STENCIL_BACK_WRITEMASK        :js.Number = ???
    val VIEWPORT                      :js.Number = ???
    val SCISSOR_BOX                   :js.Number = ???
    /*      SCISSOR_TEST */
    val COLOR_CLEAR_VALUE             :js.Number = ???
    val COLOR_WRITEMASK               :js.Number = ???
    val UNPACK_ALIGNMENT              :js.Number = ???
    val PACK_ALIGNMENT                :js.Number = ???
    val MAX_TEXTURE_SIZE              :js.Number = ???
    val MAX_VIEWPORT_DIMS             :js.Number = ???
    val SUBPIXEL_BITS                 :js.Number = ???
    val RED_BITS                      :js.Number = ???
    val GREEN_BITS                    :js.Number = ???
    val BLUE_BITS                     :js.Number = ???
    val ALPHA_BITS                    :js.Number = ???
    val DEPTH_BITS                    :js.Number = ???
    val STENCIL_BITS                  :js.Number = ???
    val POLYGON_OFFSET_UNITS          :js.Number = ???
    /*      POLYGON_OFFSET_FILL */
    val POLYGON_OFFSET_FACTOR         :js.Number = ???
    val TEXTURE_BINDING_2D            :js.Number = ???
    val SAMPLE_BUFFERS                :js.Number = ???
    val SAMPLES                       :js.Number = ???
    val SAMPLE_COVERAGE_VALUE         :js.Number = ???
    val SAMPLE_COVERAGE_INVERT        :js.Number = ???
    
    /* GetTextureParameter */
    /*      TEXTURE_MAG_FILTER */
    /*      TEXTURE_MIN_FILTER */
    /*      TEXTURE_WRAP_S */
    /*      TEXTURE_WRAP_T */
    
    val COMPRESSED_TEXTURE_FORMATS    :js.Number = ???
    
    /* HintMode */
    val DONT_CARE                     :js.Number = ???
    val FASTEST                       :js.Number = ???
    val NICEST                        :js.Number = ???
    
    /* HintTarget */
    val GENERATE_MIPMAP_HINT           :js.Number = ???
    
    /* DataType */
    val BYTE                          :js.Number = ???
    val UNSIGNED_BYTE                 :js.Number = ???
    val SHORT                         :js.Number = ???
    val UNSIGNED_SHORT                :js.Number = ???
    val INT                           :js.Number = ???
    val UNSIGNED_INT                  :js.Number = ???
    val FLOAT                         :js.Number = ???
    
    /* PixelFormat */
    val DEPTH_COMPONENT               :js.Number = ???
    val ALPHA                         :js.Number = ???
    val RGB                           :js.Number = ???
    val RGBA                          :js.Number = ???
    val LUMINANCE                     :js.Number = ???
    val LUMINANCE_ALPHA               :js.Number = ???
    
    /* PixelType */
    /*      UNSIGNED_BYTE */
    val UNSIGNED_SHORT_4_4_4_4        :js.Number = ???
    val UNSIGNED_SHORT_5_5_5_1        :js.Number = ???
    val UNSIGNED_SHORT_5_6_5          :js.Number = ???
    
    /* Shaders */
    val FRAGMENT_SHADER                 :js.Number = ???
    val VERTEX_SHADER                   :js.Number = ???
    val MAX_VERTEX_ATTRIBS              :js.Number = ???
    val MAX_VERTEX_UNIFORM_VECTORS      :js.Number = ???
    val MAX_VARYING_VECTORS             :js.Number = ???
    val MAX_COMBINED_TEXTURE_IMAGE_UNITS:js.Number = ???
    val MAX_VERTEX_TEXTURE_IMAGE_UNITS  :js.Number = ???
    val MAX_TEXTURE_IMAGE_UNITS         :js.Number = ???
    val MAX_FRAGMENT_UNIFORM_VECTORS    :js.Number = ???
    val SHADER_TYPE                     :js.Number = ???
    val DELETE_STATUS                   :js.Number = ???
    val LINK_STATUS                     :js.Number = ???
    val VALIDATE_STATUS                 :js.Number = ???
    val ATTACHED_SHADERS                :js.Number = ???
    val ACTIVE_UNIFORMS                 :js.Number = ???
    val ACTIVE_ATTRIBUTES               :js.Number = ???
    val SHADING_LANGUAGE_VERSION        :js.Number = ???
    val CURRENT_PROGRAM                 :js.Number = ???
    
    /* StencilFunction */
    val NEVER                         :js.Number = ???
    val LESS                          :js.Number = ???
    val EQUAL                         :js.Number = ???
    val LEQUAL                        :js.Number = ???
    val GREATER                       :js.Number = ???
    val NOTEQUAL                      :js.Number = ???
    val GEQUAL                        :js.Number = ???
    val ALWAYS                        :js.Number = ???
    
    /* StencilOp */
    /*      ZERO */
    val KEEP                          :js.Number = ???
    val REPLACE                       :js.Number = ???
    val INCR                          :js.Number = ???
    val DECR                          :js.Number = ???
    val INVERT                        :js.Number = ???
    val INCR_WRAP                     :js.Number = ???
    val DECR_WRAP                     :js.Number = ???
    
    /* StringName */
    val VENDOR                        :js.Number = ???
    val RENDERER                      :js.Number = ???
    val VERSION                       :js.Number = ???
    
    /* TextureMagFilter */
    val NEAREST                       :js.Number = ???
    val LINEAR                        :js.Number = ???
    
    /* TextureMinFilter */
    /*      NEAREST */
    /*      LINEAR */
    val NEAREST_MIPMAP_NEAREST        :js.Number = ???
    val LINEAR_MIPMAP_NEAREST         :js.Number = ???
    val NEAREST_MIPMAP_LINEAR         :js.Number = ???
    val LINEAR_MIPMAP_LINEAR          :js.Number = ???
    
    /* TextureParameterName */
    val TEXTURE_MAG_FILTER            :js.Number = ???
    val TEXTURE_MIN_FILTER            :js.Number = ???
    val TEXTURE_WRAP_S                :js.Number = ???
    val TEXTURE_WRAP_T                :js.Number = ???
    
    /* TextureTarget */
    val TEXTURE_2D                    :js.Number = ???
    val TEXTURE                       :js.Number = ???
    
    val TEXTURE_CUBE_MAP              :js.Number = ???
    val TEXTURE_BINDING_CUBE_MAP      :js.Number = ???
    val TEXTURE_CUBE_MAP_POSITIVE_X   :js.Number = ???
    val TEXTURE_CUBE_MAP_NEGATIVE_X   :js.Number = ???
    val TEXTURE_CUBE_MAP_POSITIVE_Y   :js.Number = ???
    val TEXTURE_CUBE_MAP_NEGATIVE_Y   :js.Number = ???
    val TEXTURE_CUBE_MAP_POSITIVE_Z   :js.Number = ???
    val TEXTURE_CUBE_MAP_NEGATIVE_Z   :js.Number = ???
    val MAX_CUBE_MAP_TEXTURE_SIZE     :js.Number = ???
    
    /* TextureUnit */
    val TEXTURE0                      :js.Number = ???
    val TEXTURE1                      :js.Number = ???
    val TEXTURE2                      :js.Number = ???
    val TEXTURE3                      :js.Number = ???
    val TEXTURE4                      :js.Number = ???
    val TEXTURE5                      :js.Number = ???
    val TEXTURE6                      :js.Number = ???
    val TEXTURE7                      :js.Number = ???
    val TEXTURE8                      :js.Number = ???
    val TEXTURE9                      :js.Number = ???
    val TEXTURE10                     :js.Number = ???
    val TEXTURE11                     :js.Number = ???
    val TEXTURE12                     :js.Number = ???
    val TEXTURE13                     :js.Number = ???
    val TEXTURE14                     :js.Number = ???
    val TEXTURE15                     :js.Number = ???
    val TEXTURE16                     :js.Number = ???
    val TEXTURE17                     :js.Number = ???
    val TEXTURE18                     :js.Number = ???
    val TEXTURE19                     :js.Number = ???
    val TEXTURE20                     :js.Number = ???
    val TEXTURE21                     :js.Number = ???
    val TEXTURE22                     :js.Number = ???
    val TEXTURE23                     :js.Number = ???
    val TEXTURE24                     :js.Number = ???
    val TEXTURE25                     :js.Number = ???
    val TEXTURE26                     :js.Number = ???
    val TEXTURE27                     :js.Number = ???
    val TEXTURE28                     :js.Number = ???
    val TEXTURE29                     :js.Number = ???
    val TEXTURE30                     :js.Number = ???
    val TEXTURE31                     :js.Number = ???
    val ACTIVE_TEXTURE                :js.Number = ???
    
    /* TextureWrapMode */
    val REPEAT                        :js.Number = ???
    val CLAMP_TO_EDGE                 :js.Number = ???
    val MIRRORED_REPEAT               :js.Number = ???
    
    /* Uniform Types */
    val FLOAT_VEC2                    :js.Number = ???
    val FLOAT_VEC3                    :js.Number = ???
    val FLOAT_VEC4                    :js.Number = ???
    val INT_VEC2                      :js.Number = ???
    val INT_VEC3                      :js.Number = ???
    val INT_VEC4                      :js.Number = ???
    val BOOL                          :js.Number = ???
    val BOOL_VEC2                     :js.Number = ???
    val BOOL_VEC3                     :js.Number = ???
    val BOOL_VEC4                     :js.Number = ???
    val FLOAT_MAT2                    :js.Number = ???
    val FLOAT_MAT3                    :js.Number = ???
    val FLOAT_MAT4                    :js.Number = ???
    val SAMPLER_2D                    :js.Number = ???
    val SAMPLER_CUBE                  :js.Number = ???
    
    /* Vertex Arrays */
    val VERTEX_ATTRIB_ARRAY_ENABLED       :js.Number = ???
    val VERTEX_ATTRIB_ARRAY_SIZE          :js.Number = ???
    val VERTEX_ATTRIB_ARRAY_STRIDE        :js.Number = ???
    val VERTEX_ATTRIB_ARRAY_TYPE          :js.Number = ???
    val VERTEX_ATTRIB_ARRAY_NORMALIZED    :js.Number = ???
    val VERTEX_ATTRIB_ARRAY_POINTER       :js.Number = ???
    val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING:js.Number = ???
    
    /* Shader Source */
    val COMPILE_STATUS                :js.Number = ???
    
    /* Shader Precision-Specified Types */
    val LOW_FLOAT                     :js.Number = ???
    val MEDIUM_FLOAT                  :js.Number = ???
    val HIGH_FLOAT                    :js.Number = ???
    val LOW_INT                       :js.Number = ???
    val MEDIUM_INT                    :js.Number = ???
    val HIGH_INT                      :js.Number = ???
    
    /* Framebuffer Object. */
    val FRAMEBUFFER                   :js.Number = ???
    val RENDERBUFFER                  :js.Number = ???
    
    val RGBA4                         :js.Number = ???
    val RGB5_A1                       :js.Number = ???
    val RGB565                        :js.Number = ???
    val DEPTH_COMPONENT16             :js.Number = ???
    val STENCIL_INDEX                 :js.Number = ???
    val STENCIL_INDEX8                :js.Number = ???
    val DEPTH_STENCIL                 :js.Number = ???
    
    val RENDERBUFFER_WIDTH            :js.Number = ???
    val RENDERBUFFER_HEIGHT           :js.Number = ???
    val RENDERBUFFER_INTERNAL_FORMAT  :js.Number = ???
    val RENDERBUFFER_RED_SIZE         :js.Number = ???
    val RENDERBUFFER_GREEN_SIZE       :js.Number = ???
    val RENDERBUFFER_BLUE_SIZE        :js.Number = ???
    val RENDERBUFFER_ALPHA_SIZE       :js.Number = ???
    val RENDERBUFFER_DEPTH_SIZE       :js.Number = ???
    val RENDERBUFFER_STENCIL_SIZE     :js.Number = ???
    
    val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE          :js.Number = ???
    val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME          :js.Number = ???
    val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL        :js.Number = ???
    val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE:js.Number = ???
    
    val COLOR_ATTACHMENT0             :js.Number = ???
    val DEPTH_ATTACHMENT              :js.Number = ???
    val STENCIL_ATTACHMENT            :js.Number = ???
    val DEPTH_STENCIL_ATTACHMENT      :js.Number = ???
    
    val NONE                          :js.Number = ???
    
    val FRAMEBUFFER_COMPLETE                     :js.Number = ???
    val FRAMEBUFFER_INCOMPLETE_ATTACHMENT        :js.Number = ???
    val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:js.Number = ???
    val FRAMEBUFFER_INCOMPLETE_DIMENSIONS        :js.Number = ???
    val FRAMEBUFFER_UNSUPPORTED                  :js.Number = ???
    
    val FRAMEBUFFER_BINDING           :js.Number = ???
    val RENDERBUFFER_BINDING          :js.Number = ???
    val MAX_RENDERBUFFER_SIZE         :js.Number = ???
    
    val INVALID_FRAMEBUFFER_OPERATION :js.Number = ???
    
    /* WebGL-specific enums */
    val UNPACK_FLIP_Y_WEBGL           :js.Number = ???
    val UNPACK_PREMULTIPLY_ALPHA_WEBGL:js.Number = ???
    val CONTEXT_LOST_WEBGL            :js.Number = ???
    val UNPACK_COLORSPACE_CONVERSION_WEBGL:js.Number = ???
    val BROWSER_DEFAULT_WEBGL         :js.Number = ???

    val canvas:HTMLCanvasElement = ???
    val drawingBufferWidth:js.Number = ???
    val drawingBufferHeight:js.Number = ???

    def clear(mask:js.Number) = ???
    def clearColor(red:js.Number, green:js.Number, blue:js.Number, alpha:js.Number) = ???
}
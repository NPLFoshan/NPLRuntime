#pragma once
#include "RenderDeviceOpenGL.h"
#include "OpenGLWrapper/GLLabel.h"
namespace ParaEngine
{
	class CSpriteRenderer;

	/** used for rendering labels 
	* rendering a given font from TTF format. It emulates the d3dxfont class.
	*/
	class CFontRendererOpenGL : public GLLabel
	{
	public:
		static CFontRendererOpenGL* create(const std::string& sFontName, int nFontSize);
		CFontRendererOpenGL();
		virtual ~CFontRendererOpenGL();
	public:
		bool DrawTextA(CSpriteRenderer* pSprite, const char* strText, RECT& rect, DWORD dwTextFormat, DWORD textColor);
		bool DrawTextW(CSpriteRenderer* pSprite, const char16_t* strText, RECT& rect, DWORD dwTextFormat, DWORD textColor);

		void SetUTF16Text(const char16_t* strText);

		bool UpdateLetterSprites(DWORD dwTextFormat);

		void RenderLetterSprites(CSpriteRenderer* pSprite, DWORD color);

		void DoRender(CSpriteRenderer* pSprite, DWORD color);

		virtual float GetFontScaling() const;

		void RendererRecreated();

	protected:
		int m_nFontSize;
		// to minimize the total number of font used, the real font-size may be smaller than the m_nFontSize, and a scale factor is used during rendering. 
		int m_nRealFontSize;
		float m_fFontScaling;
		bool m_bUseCustomShader;
	};
}


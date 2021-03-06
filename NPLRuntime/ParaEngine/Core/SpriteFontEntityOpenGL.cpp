//-----------------------------------------------------------------------------
// Class:	SpriteFontEntity
// Authors:	LiXizhi
// Emails:	LiXizhi@yeah.net
// Company: ParaEngine
// Date:	2004.3.8
// Revised: 2006.7.12, 2014.8(cross platform)
//-----------------------------------------------------------------------------
#include "ParaEngine.h"
#ifdef USE_OPENGL_RENDERER
#include "RenderDeviceOpenGL.h"
#include "StringHelper.h"
#include "2dengine/GUIBase.h"
#include "SpriteFontEntity.h"
#include "memdebug.h"

using namespace ParaEngine;

SpriteFontEntityOpenGL::SpriteFontEntityOpenGL(const AssetKey& key)
:SpriteFontEntity(key), m_fontRenderer(NULL)
{
}
SpriteFontEntityOpenGL::SpriteFontEntityOpenGL()
: m_fontRenderer(NULL)
{
}

SpriteFontEntityOpenGL::~SpriteFontEntityOpenGL()
{
}

void ParaEngine::SpriteFontEntityOpenGL::Cleanup()
{
	SAFE_RELEASE(m_fontRenderer);
}

float ParaEngine::SpriteFontEntityOpenGL::GetFontScaling()
{
	return m_fontRenderer ? m_fontRenderer->GetFontScaling() : 1.0f;
}

CFontRendererOpenGL* ParaEngine::SpriteFontEntityOpenGL::GetLabel()
{
	LoadAsset();
	return m_fontRenderer;
}



HRESULT ParaEngine::SpriteFontEntityOpenGL::DrawTextW(CSpriteRenderer* pSprite, const char16_t* strText, int nCount, RECT* rect, DWORD dwTextFormat, DWORD textColor)
{
	LoadAsset();
	if (m_fontRenderer)
		m_fontRenderer->DrawTextW(pSprite, strText, *rect, dwTextFormat, textColor);
	return S_OK;
}

HRESULT ParaEngine::SpriteFontEntityOpenGL::DrawTextA(CSpriteRenderer* pSprite, const char* strText, int nCount, RECT* rect, DWORD dwTextFormat, DWORD textColor)
{
	LoadAsset();
	if (m_fontRenderer)
		m_fontRenderer->DrawTextA(pSprite, strText, *rect, dwTextFormat, textColor);
	return S_OK;
}

HRESULT ParaEngine::SpriteFontEntityOpenGL::InitDeviceObjects()
{
	if (m_bIsInitialized)
		return S_OK;
	m_bIsInitialized = true;
	SAFE_RELEASE(m_fontRenderer);
	m_fontRenderer = CFontRendererOpenGL::create(GetFontName(), GetFontSize());
	if(m_fontRenderer)
		m_fontRenderer->addref();
	else
	{
		OUTPUT_LOG("warning: failed to init font entity: %s;%d\n", GetFontName().c_str(), GetFontSize());
	}

	return S_OK;
}

HRESULT ParaEngine::SpriteFontEntityOpenGL::DeleteDeviceObjects()
{
	m_bIsInitialized = false;
	SAFE_RELEASE(m_fontRenderer);
	return S_OK;
}

HRESULT ParaEngine::SpriteFontEntityOpenGL::RendererRecreated()
{
	// the default implementation of CCLabel's FontAtlas does not seem to handle renderer creation very well
	// we just delete and create again. 
	m_fontRenderer->RendererRecreated();
	DeleteDeviceObjects();
	InitDeviceObjects();
	return S_OK;
}

#endif
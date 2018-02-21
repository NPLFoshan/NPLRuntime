#pragma once
#include "IParaEngineApp.h"
#include "CommandLineParams.h"
#include "ParaVector2.h"
#include "FrameRateController.h"
namespace ParaEngine
{
	class ParaTimer;
	class CObjectAutoReleasePool;

	/* base implementation of ParaEngine APP, shared by both mobile and pc version.  */
	class CParaEngineAppBase : public IParaEngineApp, public CCommandLineParams, public IObject
	{

	public:
		CParaEngineAppBase();
		CParaEngineAppBase(const char* sCmd);
		virtual ~CParaEngineAppBase();
		static IParaEngineApp* GetInstance();
		virtual bool InitApp(IRenderWindow* pWindow, const char* sCommandLine /* = nullptr */) override;
		virtual bool StartApp() override;
		virtual void StopApp() override;

		virtual void Enable3DRendering(bool bEnable) override;
		virtual bool Is3DRenderingEnabled()override;
		/** whether the last mouse input is from touch or mouse. by default it is mouse mode. */
		virtual bool IsTouchInputting() override;
		virtual bool IsSlateMode() override;
		virtual void DoWork();
		virtual bool FrameMove(double fTime) override;


	protected:
		void SetTouchInputting(bool bTouchInputting);
		void AutoSetLocale();
		/** call this function at the end of the frame. */
		virtual void OnFrameEnded();
		virtual void WriteToLog(const char* sFormat, ...);
		virtual void AppLog(const char* sFormat);
		/** set the current working directory. This function is called in the constructor to ensure that all IO directs to the right dir. */
		virtual bool FindParaEngineDirectory(const char* sHint = NULL);
		void BootStrapAndLoadConfig();
		void InitSystemModules();
		void InitRenderEnvironment();
		void ResetRenderEnvironment();

		void InitDeviceObjects();
		void DeleteDeviceObjects();
		void RestoreDeviceObjects();
		void InvalidateDeviceObjects();
		void Render();
		void HandleUserInput();
		

	protected:
		static IParaEngineApp* g_pCurrentApp;
		bool m_bEnable3DRendering;
		bool m_isTouching;
		bool m_hasClosingRequest;
		/** the application exit code or return code. 0 means success. otherwise means a failure. */
		int m_nReturnCode;
		/** a pool of registered singleton object. */
		CObjectAutoReleasePool* m_pSingletonReleasePool;
		/** packages/ directory path */
		std::string m_sPackagesDir;
		/** bin/ module path */
		std::string m_sModuleDir;
		/** initial working directory*/
		std::string m_sInitialWorkingDir;
		/** application state */
		PEAppState m_nAppState;
		ref_ptr<CGUIRoot> m_pGUIRoot;
		/** 3d scene root object */
		ref_ptr<CSceneObject>		  m_pRootScene;
		/** viewport */
		ref_ptr<CViewportManager>	  m_pViewportManager;
		/** asset manager */
		ref_ptr<CParaWorldAsset>	  m_pParaWorldAsset;
		ParaTimer* m_Timer;
		IRenderContext* m_pRenderContext;
		IRenderDevice* m_pRenderDevice;
		IRenderWindow* m_pRenderWindow;
		CFrameRateController m_doWorkFRC;

#pragma region OLD_CODE


	public:

		/** switch to ignore windows size change. default to false.
		* if false, the user is allowed to adjust window size in windowed mode. */
		virtual void SetIgnoreWindowSizeChange(bool bIgnoreSizeChange){};

		/** return true if it is currently under windowed mode. */
		virtual bool GetIgnoreWindowSizeChange(){ return false; };

		/** get the module handle, it may be exe or the dll handle, depending on how the main host app is built. */
		HINSTANCE GetModuleHandle() { return 0; };

		/**
		* Set the frame rate timer interval
		* @param fTimeInterval:  value in seconds. such as 0.033f or 0.01667f
		* 	Passing a value <= 0 to render in idle time.
		* @param nFrameRateControl: 0 for real time, 1 for ideal frame rate at 30 FPS no matter whatever time interval is set.
		*/
		virtual void SetRefreshTimer(float fTimeInterval, int nFrameRateControl = 0) { };

		/** get the refresh timer.
		*/
		virtual float GetRefreshTimer()  { return 0; };

		/** this function is called whenever the application is disabled or enabled. usually called when receiving the WM_ACTIVATEAPP message.
		* [main thread only]
		*/
		virtual void ActivateApp(bool bActivate) { };

		/** whether the application is active or not. */
		virtual bool IsAppActive();

		/** Get the current ParaEngine app usage.
		* [main thread only]
		* @return see PE_USAGE
		*/
		virtual DWORD GetCoreUsage();

		/** Set the current ParaEngine app usage.
		* [main thread only]
		* @param dwUsage: bitwise of PE_USAGE
		*/
		virtual void SetCoreUsage(DWORD dwUsage) { };


		/** Get the exit code that will be used when the standalone executable exit.
		* this is mainly used for writing test cases. Where a return value of 0 means success, any other value means failure.
		*/
		virtual void SetReturnCode(int nReturnCode);
		virtual int GetReturnCode();

		/** show a system message box to the user. mostly about fatal error.  */
		virtual void SystemMessageBox(const std::string& msg);

		/** set string specifying the command line for the application, excluding the program name.
		* calling this function multiple times with different command line is permitted. The latter settings will merge and override the previous ones.
		* @param pCommandLine: such as   key="value" key2="value2"
		*/
		virtual void SetAppCommandLine(const char* pCommandLine);

		/** get string specifying the command line for the application, excluding the program name. */
		virtual const char* GetAppCommandLine();

		/**
		* return a specified parameter value in the command line of the application. If the parameter does not exist, the return value is NULL.
		* @param pParam: key to get
		* @param defaultValue: if the key does not exist, this value will be added and returned. This can be NULL.
		*/
		virtual const char* GetAppCommandLineByParam(const char* pParam, const char* defaultValue);
	public:


		/** Send the exit message, so that the game engine will prepare to exit in the next frame.
		* this is the recommended way of exiting application.
		* this is mainly used for writing test cases. Where a return value of 0 means success, any other value means failure.
		*/
		virtual void Exit(int nReturnCode = 0);

		virtual ParaEngine::PEAppState GetAppState();
		virtual void SetAppState(ParaEngine::PEAppState state);


		/**
		* This function should be called only once when the application end, one can destroy game objects here.
		*/
		virtual bool FinalCleanup();


		/** only call this function if one does not want to manage game loop externally. */
		virtual int Run(HINSTANCE hInstance){ return 0; };


		/** create d3d render device based on the current main window.
		* Use this function to create a new render device automatically.
		*/
		virtual HRESULT Create(HINSTANCE hInstance = 0)  { return 0; };

		/** Frame move and render a frame during idle time (no messages are waiting). Call this function during CPU idle time.
		* internally it uses a timer to control frame rates, so it is safe to call this as often as one like.
		* @param bForceRender: if true, it will force frame move and render the scene. if not, it will
		* internally use a frame rate controller that maintain the frame rate at 30 fps, no matter who often this function is called.
		*/
		virtual HRESULT Render3DEnvironment(bool bForceRender = false) { return S_OK; }

		/** the window message processor. One needs send all messages belonging to the main window to this function, after calling Create().
		* @note: the main rendering thread can be a different thread than the window proc thread.
		* @param bCallDefProcedure: whether we will call the ::DefWindowProdure().
		* @return: 0 if message was not processed. 1 if message is processed. -1 if message is processed by can be passed on to other processor.
		*/
		virtual LRESULT MsgProcWinThread(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam, bool bCallDefProcedure = true)  { return 0; };

		/*** process win thread messages without a hWnd. Such messages are usually invoked by PostWinThreadMessage() from other threads. */
		virtual LRESULT MsgProcWinThreadCustom(UINT uMsg, WPARAM wParam, LPARAM lParam)  { return 0; };

		/** Send a RAW win32 message the application to be processed in the next main thread update interval.
		* This function can be called from any thread. It is also used by the windows procedure thread to dispatch messages to the main processing thread.
		*/
		virtual LRESULT SendMessageToApp(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)  { return 0; };

		/** post a raw win32 message from any thread to the thread on which hWnd is created. */
		virtual bool PostWinThreadMessage(UINT uMsg, WPARAM wParam, LPARAM lParam)  { return false; };

		/** get a message from the application message queue and remove it from the queue. This function is mostly
		used internally by the main thread.
		* @param pMsg: the receiving message
		* @return true if one message is fetched. or false if there is no more messages in the queue.
		*/
		virtual bool GetMessageFromApp(CWinRawMsg* pMsg)  { return false; };

		/**
		* handle a message in the main application thread.
		*/
		virtual LRESULT MsgProcApp(HWND hWnd, UINT uMsg, WPARAM wParam, LPARAM lParam)  { return 0; };

		/**  passive rendering, it will not render the scene, but simulation and time remains the same. Default is false*/
		virtual void    EnablePassiveRendering(bool bEnable) { };

		/**  passive rendering, it will not render the scene, but simulation and time remains the same. Default is false*/
		virtual bool	IsPassiveRenderingEnabled()  { return false; };

		/** The BringWindowToTop current window to the top of the Z order. This function only works if IsFullScreenMode() is false.
		*/
		virtual void BringWindowToTop() { };

		/** whether the user can close the window (sending WM_CLOSE message). Default to true.
		* When set to false, the scripting interface will receive WM_CLOSE message via system event. And can use ParaEngine.Exit() to quit the application after user confirmation, etc.
		*/
		virtual void SetAllowWindowClosing(bool bAllowClosing) { };
		virtual bool IsWindowClosingAllowed()  { return false; };

		/** whether to use full screen mode, it does not immediately change the device, call UpdateScreenMode() to update the device. */
		virtual void SetFullScreenMode(bool bFullscreen){};
		virtual bool IsFullScreenMode(){ return false; };

		/** change the full screen mode, it does not immediately change the device, call UpdateScreenMode() to update the device. */
		virtual void GetScreenResolution(Vector2* pOut)  { *pOut = Vector2::ZERO; };
		virtual void SetScreenResolution(const Vector2& vSize){};

		/** anti-aliasing for both windowed and full screen mode. it does not immediately change the device, call UpdateScreenMode() to update the device.*/
		virtual int GetMultiSampleType(){ return 0; };
		virtual void SetMultiSampleType(int nType){};

		/** anti-aliasing for both windowed and full screen mode. it does not immediately change the device, call UpdateScreenMode() to update the device.*/
		virtual int GetMultiSampleQuality(){ return 0; };
		virtual void SetMultiSampleQuality(int nType){};

		/** call this function to update changes of FullScreen Mode and Screen Resolution. */
		virtual bool UpdateScreenMode(){ return false; };

		// ParaEngine pipeline routines
		/** switch to either windowed mode or full screen mode. */
		virtual bool SetWindowedMode(bool bWindowed){ return false; };
		/** return true if it is currently under windowed mode. */
		virtual bool IsWindowedMode(){ return true; };
		/** set the window title when at windowed mode */
		virtual void SetWindowText(const char* pChar) {};
		/** get the window title when at windowed mode */
		virtual const char* GetWindowText() { return ""; };
		/* */
		virtual void FixWindowSize(bool fixed) {};


		/** write the current setting to config file. Such as graphics mode and whether full screen, etc.
		* config file at ./config.txt will be automatically loaded when the game engine starts.
		* @param sFileName: if this is "", it will be the default config file at ./config.txt
		*/
		virtual void WriteConfigFile(const char* sFileName) {};

		/** get whether ParaEngine is loaded from config/config.new.txt. if this is true, we need to ask the user to save to config/config.txt. This is usually done at start up. */
		virtual bool HasNewConfig() { return false; };

		/** set whether ParaEngine is loaded from config/config.new.txt. if this is true, we need to ask the user to save to config/config.txt. This is usually done at start up. */
		virtual void SetHasNewConfig(bool bHasNewConfig) {};


		/** get the window creation size in default application config. */
		virtual void GetWindowCreationSize(int * pWidth, int * pHeight) {};

		/** set the minimum UI resolution size. if the backbuffer is smaller than this, we will use automatically use UI scaling
		* for example, if minimum width is 1024, and backbuffer it 800, then m_fUIScalingX will be automatically set to 1024/800.
		* calling this function will cause OnSize() and UpdateBackbufferSize() to be called. Actually it calls SetUIScale()
		* [main thread only]
		* @param nWidth: the new width.
		* @param nHeight: the new height.
		* @param bAutoUIScaling: default to true. whether we will automatically recalculate the UI scaling accordingly with regard to current backbuffer size.
		*/
		virtual void SetMinUIResolution(int nWidth, int nHeight, bool bAutoUIScaling = true) { };

		/** change the full screen mode, it does not immediately change the device, call UpdateScreenMode() to update the device. */
		virtual void GetResolution(float* pX, float* pY) {};
		virtual void SetResolution(float x, float y) {};

		/** get the NPL runtime system associate with the application. NPL provides communication framework across different language systems. */
		virtual NPL::INPLRuntime* GetNPLRuntime();

		/**
		* we will automatically release singleton object when app stops, in the verse order when object is added to the pool.
		*/
		virtual CRefCounted* AddToSingletonReleasePool(CRefCounted* pObject);

		/** whether there is closing request in the message queue. In windows, the user clicks the close button.
		* For long running task in the main thread, it is recommended to periodically check this value to end the task prematurely if necessary.
		*/
		virtual bool HasClosingRequest();
		virtual void SetHasClosingRequest(bool val);

		/** load NPL package from a disk folder.
		* it will first search the dev folder, then the current folder, and then the executable folder and all of its parent folders.
		* Once the folder is found, it is added to the global search path.
		* @param sFilePath: for example, "npl_packages/main/" is always loaded on start up.
		* @param pOutMainFile: output of the actual folder name or a main loader file path in the main loader. 
		*/
		virtual bool LoadNPLPackage(const char* sFilePath, std::string * pOutMainFile = NULL);

		/** check if there is bootstrapper file specified at command line, if not it will use NPL code wiki admin app. 
		*/
		virtual bool FindBootStrapper();

		/** parse common command line parameters */
		virtual bool InitCommandLineParams();


		/** render the current frame and does not return until everything is presented to screen.
		* this function is usually used to draw the animated loading screen. */
		virtual bool ForceRender();

		/** get the NPL bin directory (main executable directory). this one ends with "/" */
		virtual const char* GetModuleDir();;
	public:
		/** managing multiple 3d views */
		CViewportManager * GetViewportManager();

		virtual void VerifyCommandLine(const char* sCommandLine, std::string &strCmd);

	protected:
		/** shared init called in constructor.  */
		void DestroySingletons();

		/** we will load all packages that matches the following pattern in the order given by their name,
		* such that "main_001.pkg" is always loaded before "main_002.pkg" 
		* we will skip packages *_32bits.pkg when in 64bits; and skip *_64bits.pkg in 32bits; packages without above suffix will be loaded in both version. 
		*/
		void LoadPackages();
		void LoadPackagesInFolder(const std::string& sPkgFolder);
		
		/** register a given class. */
		void RegisterObjectClass(IAttributeFields* pObject);

		/** register any custom classes */
		void RegisterObjectClasses();

	};


#pragma endregion OLD_CODE

}


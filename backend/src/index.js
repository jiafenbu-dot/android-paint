/**
 * AndroidPaint Cloudflare Worker 后端 API
 * 
 * 功能：
 * - 用户认证（邮箱注册/登录、手机号登录、微信登录）
 * - 项目管理（CRUD）
 * - 云端同步（版本控制、冲突检测）
 * - R2 存储（ORA 文件和缩略图）
 */

// ==================== 辅助函数 ====================

/**
 * 返回 JSON 响应
 */
function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' }
  });
}

/**
 * 返回错误响应
 */
function errorResponse(message, status = 400) {
  return jsonResponse({ error: message }, status);
}

/**
 * 生成 UUID v4
 */
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

/**
 * 生成随机验证码
 */
function generateCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

/**
 * 获取当前时间戳（秒）
 */
function getTimestamp() {
  return Math.floor(Date.now() / 1000);
}

// ==================== JWT 实现 ====================

/**
 * 使用 Web Crypto API 实现 HMAC-SHA256 签名
 */
async function signJWT(payload, secret) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const headerEncoded = btoa(JSON.stringify(header));
  const payloadEncoded = btoa(JSON.stringify(payload));
  
  const signingInput = `${headerEncoded}.${payloadEncoded}`;
  
  const encoder = new TextEncoder();
  const keyData = encoder.encode(secret);
  const signingInputData = encoder.encode(signingInput);
  
  const key = await crypto.subtle.importKey(
    'raw',
    keyData,
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  
  const signature = await crypto.subtle.sign('HMAC', key, signingInputData);
  const signatureBase64 = btoa(String.fromCharCode(...new Uint8Array(signature)));
  
  return `${signingInput}.${signatureBase64}`;
}

/**
 * 验证并解码 JWT
 */
async function verifyJWT(token, secret) {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    
    const [headerEncoded, payloadEncoded, signatureEncoded] = parts;
    const signingInput = `${headerEncoded}.${payloadEncoded}`;
    
    const encoder = new TextEncoder();
    const keyData = encoder.encode(secret);
    const signingInputData = encoder.encode(signingInput);
    
    const key = await crypto.subtle.importKey(
      'raw',
      keyData,
      { name: 'HMAC', hash: 'SHA-256' },
      false,
      ['sign']
    );
    
    const expectedSignature = await crypto.subtle.sign('HMAC', key, signingInputData);
    const expectedSignatureBase64 = btoa(String.fromCharCode(...new Uint8Array(expectedSignature)));
    
    if (signatureEncoded !== expectedSignatureBase64) return null;
    
    const payload = JSON.parse(atob(payloadEncoded));
    return payload;
  } catch (e) {
    return null;
  }
}

// ==================== 密码哈希实现（PBKDF2） ====================

/**
 * 使用 PBKDF2 哈希密码
 */
async function hashPassword(password, salt) {
  const encoder = new TextEncoder();
  const passwordData = encoder.encode(password);
  const saltData = encoder.encode(salt);
  
  const key = await crypto.subtle.importKey(
    'raw',
    passwordData,
    'PBKDF2',
    false,
    ['deriveBits']
  );
  
  const derivedBits = await crypto.subtle.deriveBits(
    {
      name: 'PBKDF2',
      salt: saltData,
      iterations: 100000,
      hash: 'SHA-256'
    },
    key,
    256
  );
  
  return btoa(String.fromCharCode(...new Uint8Array(derivedBits)));
}

/**
 * 生成随机盐
 */
function generateSalt() {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array));
}

/**
 * 验证密码
 */
async function verifyPassword(password, salt, hash) {
  const computedHash = await hashPassword(password, salt);
  return computedHash === hash;
}

// ==================== 认证中间件 ====================

/**
 * 验证请求中的 JWT Token
 * @returns userId 或 null
 */
async function authenticate(request, env) {
  const authHeader = request.headers.get('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return null;
  }
  
  const token = authHeader.substring(7);
  const payload = await verifyJWT(token, env.JWT_SECRET);
  
  if (!payload) return null;
  
  // 检查过期时间
  if (payload.exp && payload.exp < getTimestamp()) {
    return null;
  }
  
  return payload.userId;
}

// ==================== 验证函数 ====================

/**
 * 验证邮箱格式
 */
function isValidEmail(email) {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}

/**
 * 验证手机号格式（中国大陆）
 */
function isValidPhone(phone) {
  const regex = /^1[3-9]\d{9}$/;
  return regex.test(phone);
}

/**
 * 验证密码强度（至少8位，包含数字和字母）
 */
function isValidPassword(password) {
  return password.length >= 8 && 
         /[A-Za-z]/.test(password) && 
         /[0-9]/.test(password);
}

// ==================== Handler 函数 ====================

/**
 * 处理邮箱注册
 */
async function handleRegister(request, env) {
  try {
    const body = await request.json();
    const { email, password } = body;
    
    // 验证输入
    if (!email || !password) {
      return errorResponse('邮箱和密码不能为空');
    }
    
    if (!isValidEmail(email)) {
      return errorResponse('邮箱格式不正确');
    }
    
    if (!isValidPassword(password)) {
      return errorResponse('密码至少8位，必须包含数字和字母');
    }
    
    // 检查邮箱是否已存在
    const existingUserId = await env.KV.get(`email:${email}`);
    if (existingUserId) {
      return errorResponse('该邮箱已被注册');
    }
    
    // 创建用户
    const userId = generateUUID();
    const salt = generateSalt();
    const passwordHash = await hashPassword(password, salt);
    const now = getTimestamp();
    
    // 存储用户数据
    const userData = {
      id: userId,
      email,
      salt,
      passwordHash,
      createdAt: now,
      updatedAt: now
    };
    await env.KV.put(`user:${userId}`, JSON.stringify(userData));
    
    // 存储邮箱索引
    await env.KV.put(`email:${email}`, userId);
    
    // 初始化空项目列表
    await env.KV.put(`projects:${userId}`, JSON.stringify([]));
    
    // 生成 Token
    const accessToken = await signJWT({
      userId,
      type: 'access',
      iat: now,
      exp: now + 7 * 24 * 60 * 60  // 7 天
    }, env.JWT_SECRET);
    
    const refreshToken = await signJWT({
      userId,
      type: 'refresh',
      iat: now,
      exp: now + 30 * 24 * 60 * 60  // 30 天
    }, env.JWT_SECRET);
    
    return jsonResponse({
      success: true,
      userId,
      accessToken,
      refreshToken
    });
  } catch (e) {
    console.error('Register error:', e);
    return errorResponse('注册失败: ' + e.message, 500);
  }
}

/**
 * 处理邮箱登录
 */
async function handleLogin(request, env) {
  try {
    const body = await request.json();
    const { email, password } = body;
    
    if (!email || !password) {
      return errorResponse('邮箱和密码不能为空');
    }
    
    // 查找用户
    const userId = await env.KV.get(`email:${email}`);
    if (!userId) {
      return errorResponse('邮箱或密码错误');
    }
    
    const userData = JSON.parse(await env.KV.get(`user:${userId}`));
    
    // 验证密码
    const isValid = await verifyPassword(password, userData.salt, userData.passwordHash);
    if (!isValid) {
      return errorResponse('邮箱或密码错误');
    }
    
    const now = getTimestamp();
    
    // 生成 Token
    const accessToken = await signJWT({
      userId,
      type: 'access',
      iat: now,
      exp: now + 7 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    const refreshToken = await signJWT({
      userId,
      type: 'refresh',
      iat: now,
      exp: now + 30 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    return jsonResponse({
      success: true,
      userId,
      accessToken,
      refreshToken
    });
  } catch (e) {
    console.error('Login error:', e);
    return errorResponse('登录失败: ' + e.message, 500);
  }
}

/**
 * 发送手机验证码
 */
async function handlePhoneSendCode(request, env) {
  try {
    const body = await request.json();
    const { phone } = body;
    
    if (!phone) {
      return errorResponse('手机号不能为空');
    }
    
    if (!isValidPhone(phone)) {
      return errorResponse('手机号格式不正确');
    }
    
    // 生成验证码
    const code = generateCode();
    const now = getTimestamp();
    const expiresAt = now + 5 * 60;  // 5 分钟过期
    
    // 存储验证码（开发阶段用日志输出）
    const smsData = {
      code,
      expiresAt,
      createdAt: now
    };
    await env.KV.put(`sms:${phone}`, JSON.stringify(smsData), { expirationTtl: 300 });
    
    // 开发阶段日志输出验证码
    console.log(`[DEV] SMS Code for ${phone}: ${code}`);
    
    // TODO: 后续接入阿里云短信或其他短信服务商
    // await sendSMS(phone, code);
    
    return jsonResponse({
      success: true,
      message: '验证码已发送',
      // 开发阶段返回验证码方便测试
      devCode: code
    });
  } catch (e) {
    console.error('Send SMS error:', e);
    return errorResponse('发送验证码失败: ' + e.message, 500);
  }
}

/**
 * 处理手机号登录
 */
async function handlePhoneLogin(request, env) {
  try {
    const body = await request.json();
    const { phone, code } = body;
    
    if (!phone || !code) {
      return errorResponse('手机号和验证码不能为空');
    }
    
    // 验证验证码
    const smsData = JSON.parse(await env.KV.get(`sms:${phone}`) || '{}');
    
    if (!smsData.code || smsData.code !== code) {
      return errorResponse('验证码错误');
    }
    
    if (smsData.expiresAt < getTimestamp()) {
      return errorResponse('验证码已过期');
    }
    
    // 删除已使用的验证码
    await env.KV.delete(`sms:${phone}`);
    
    // 查找或创建用户
    let userId = await env.KV.get(`phone:${phone}`);
    let isNewUser = false;
    
    if (!userId) {
      // 创建新用户
      isNewUser = true;
      userId = generateUUID();
      const now = getTimestamp();
      
      const userData = {
        id: userId,
        phone,
        createdAt: now,
        updatedAt: now
      };
      await env.KV.put(`user:${userId}`, JSON.stringify(userData));
      await env.KV.put(`phone:${phone}`, userId);
      
      // 初始化空项目列表
      await env.KV.put(`projects:${userId}`, JSON.stringify([]));
    }
    
    const now = getTimestamp();
    
    // 生成 Token
    const accessToken = await signJWT({
      userId,
      type: 'access',
      iat: now,
      exp: now + 7 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    const refreshToken = await signJWT({
      userId,
      type: 'refresh',
      iat: now,
      exp: now + 30 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    return jsonResponse({
      success: true,
      userId,
      isNewUser,
      accessToken,
      refreshToken
    });
  } catch (e) {
    console.error('Phone login error:', e);
    return errorResponse('登录失败: ' + e.message, 500);
  }
}

/**
 * 处理微信登录
 */
async function handleWechatLogin(request, env) {
  try {
    const body = await request.json();
    const { code } = body;
    
    if (!code) {
      return errorResponse('微信授权码不能为空');
    }
    
    // 调用微信 API 换取 openid
    const wechatUrl = `https://api.weixin.qq.com/sns/oauth2/access_token?appid=${env.WECHAT_APP_ID}&secret=${env.WECHAT_APP_SECRET}&code=${code}&grant_type=authorization_code`;
    
    const wechatResponse = await fetch(wechatUrl);
    const wechatData = await wechatResponse.json();
    
    if (wechatData.errcode) {
      console.error('Wechat API error:', wechatData);
      return errorResponse('微信授权失败: ' + wechatData.errmsg);
    }
    
    const openid = wechatData.openid;
    const unionid = wechatData.unionid;  // 可能为空
    
    // 查找或创建用户
    let userId = await env.KV.get(`wechat:${openid}`);
    let isNewUser = false;
    
    if (!userId) {
      // 创建新用户
      isNewUser = true;
      userId = generateUUID();
      const now = getTimestamp();
      
      const userData = {
        id: userId,
        wechatOpenId: openid,
        wechatUnionId: unionid,
        createdAt: now,
        updatedAt: now
      };
      await env.KV.put(`user:${userId}`, JSON.stringify(userData));
      await env.KV.put(`wechat:${openid}`, userId);
      
      // 初始化空项目列表
      await env.KV.put(`projects:${userId}`, JSON.stringify([]));
    }
    
    const now = getTimestamp();
    
    // 生成 Token
    const accessToken = await signJWT({
      userId,
      type: 'access',
      iat: now,
      exp: now + 7 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    const refreshToken = await signJWT({
      userId,
      type: 'refresh',
      iat: now,
      exp: now + 30 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    return jsonResponse({
      success: true,
      userId,
      isNewUser,
      accessToken,
      refreshToken
    });
  } catch (e) {
    console.error('Wechat login error:', e);
    return errorResponse('微信登录失败: ' + e.message, 500);
  }
}

/**
 * 处理刷新 Token
 */
async function handleRefreshToken(request, env) {
  try {
    const body = await request.json();
    const { refreshToken } = body;
    
    if (!refreshToken) {
      return errorResponse('refreshToken 不能为空');
    }
    
    // 验证 refresh token
    const payload = await verifyJWT(refreshToken, env.JWT_SECRET);
    
    if (!payload) {
      return errorResponse('无效的 refresh token', 401);
    }
    
    if (payload.type !== 'refresh') {
      return errorResponse('token 类型错误', 401);
    }
    
    if (payload.exp && payload.exp < getTimestamp()) {
      return errorResponse('refresh token 已过期', 401);
    }
    
    const userId = payload.userId;
    const now = getTimestamp();
    
    // 生成新的 access token
    const accessToken = await signJWT({
      userId,
      type: 'access',
      iat: now,
      exp: now + 7 * 24 * 60 * 60
    }, env.JWT_SECRET);
    
    return jsonResponse({
      success: true,
      accessToken
    });
  } catch (e) {
    console.error('Refresh token error:', e);
    return errorResponse('刷新 token 失败: ' + e.message, 500);
  }
}

/**
 * 处理项目相关路由
 */
async function handleProjects(request, env, path, method, userId) {
  const pathParts = path.split('/').filter(Boolean);
  // pathParts: ['api', 'projects', ':id', ...]
  
  // 获取项目列表
  if (path === '/api/projects' && method === 'GET') {
    const projectIds = JSON.parse(await env.KV.get(`projects:${userId}`) || '[]');
    const projects = [];
    
    for (const projectId of projectIds) {
      const projectData = await env.KV.get(`project:${userId}:${projectId}`);
      if (projectData) {
        projects.push(JSON.parse(projectData));
      }
    }
    
    // 按修改时间倒序排列
    projects.sort((a, b) => b.modifiedAt - a.modifiedAt);
    
    return jsonResponse({ projects });
  }
  
  // 创建项目
  if (path === '/api/projects' && method === 'POST') {
    const body = await request.json();
    const { name, width, height } = body;
    
    if (!name || !width || !height) {
      return errorResponse('项目名称、宽度和高度不能为空');
    }
    
    const projectId = generateUUID();
    const now = getTimestamp();
    
    const projectData = {
      id: projectId,
      name,
      width,
      height,
      layerCount: 1,
      fileSize: 0,
      version: 1,
      createdAt: now,
      modifiedAt: now
    };
    
    // 存储项目元数据
    await env.KV.put(`project:${userId}:${projectId}`, JSON.stringify(projectData));
    
    // 添加到用户项目列表
    const projectIds = JSON.parse(await env.KV.get(`projects:${userId}`) || '[]');
    projectIds.push(projectId);
    await env.KV.put(`projects:${userId}`, JSON.stringify(projectIds));
    
    return jsonResponse({ project: projectData }, 201);
  }
  
  // 处理单个项目
  if (pathParts.length === 3 && pathParts[0] === 'api' && pathParts[1] === 'projects') {
    const projectId = pathParts[2];
    
    // 获取项目
    if (method === 'GET') {
      const projectData = await env.KV.get(`project:${userId}:${projectId}`);
      if (!projectData) {
        return errorResponse('项目不存在', 404);
      }
      return jsonResponse({ project: JSON.parse(projectData) });
    }
    
    // 更新项目
    if (method === 'PUT') {
      const body = await request.json();
      const projectData = JSON.parse(await env.KV.get(`project:${userId}:${projectId}`) || '{}');
      
      if (!projectData.id) {
        return errorResponse('项目不存在', 404);
      }
      
      const now = getTimestamp();
      const updatedData = {
        ...projectData,
        ...body,
        modifiedAt: now,
        version: (projectData.version || 0) + 1
      };
      
      await env.KV.put(`project:${userId}:${projectId}`, JSON.stringify(updatedData));
      
      // 记录版本
      await env.KV.put(`version:${userId}:${projectId}:${updatedData.version}`, JSON.stringify({
        timestamp: now,
        deviceId: request.headers.get('X-Device-Id') || 'unknown'
      }));
      
      return jsonResponse({ project: updatedData });
    }
    
    // 删除项目
    if (method === 'DELETE') {
      const projectData = await env.KV.get(`project:${userId}:${projectId}`);
      if (!projectData) {
        return errorResponse('项目不存在', 404);
      }
      
      // 删除元数据
      await env.KV.delete(`project:${userId}:${projectId}`);
      
      // 从用户项目列表移除
      const projectIds = JSON.parse(await env.KV.get(`projects:${userId}`) || '[]');
      const newProjectIds = projectIds.filter(id => id !== projectId);
      await env.KV.put(`projects:${userId}`, JSON.stringify(newProjectIds));
      
      // 删除 R2 中的文件
      try {
        await env.R2.delete(`${userId}/${projectId}.ora`);
        await env.R2.delete(`${userId}/${projectId}.thumb.png`);
      } catch (e) {
        console.error('Failed to delete R2 files:', e);
      }
      
      return jsonResponse({ success: true });
    }
  }
  
  // 上传项目文件
  if (path.match(/^\/api\/projects\/[^/]+\/upload$/) && method === 'PUT') {
    const projectId = pathParts[2];
    const deviceId = request.headers.get('X-Device-Id') || 'unknown';
    
    const projectData = JSON.parse(await env.KV.get(`project:${userId}:${projectId}`) || '{}');
    if (!projectData.id) {
      return errorResponse('项目不存在', 404);
    }
    
    // 获取文件数据
    const fileData = await request.arrayBuffer();
    
    // 上传到 R2
    await env.R2.put(`${userId}/${projectId}.ora`, fileData);
    
    // 更新项目大小
    const now = getTimestamp();
    projectData.fileSize = fileData.byteLength;
    projectData.modifiedAt = now;
    projectData.version = (projectData.version || 0) + 1;
    await env.KV.put(`project:${userId}:${projectId}`, JSON.stringify(projectData));
    
    // 记录版本
    await env.KV.put(`version:${userId}:${projectId}:${projectData.version}`, JSON.stringify({
      timestamp: now,
      deviceId
    }));
    
    // 尝试解析并更新缩略图
    try {
      // 这里可以添加缩略图提取逻辑
    } catch (e) {
      console.error('Failed to extract thumbnail:', e);
    }
    
    return jsonResponse({ success: true, version: projectData.version });
  }
  
  // 下载项目文件
  if (path.match(/^\/api\/projects\/[^/]+\/download$/) && method === 'GET') {
    const projectId = pathParts[2];
    
    try {
      const file = await env.R2.get(`${userId}/${projectId}.ora`);
      if (!file) {
        return errorResponse('文件不存在', 404);
      }
      
      return new Response(file.body, {
        headers: {
          'Content-Type': 'application/octet-stream',
          'Content-Disposition': `attachment; filename="${projectId}.ora"`
        }
      });
    } catch (e) {
      return errorResponse('下载失败: ' + e.message, 500);
    }
  }
  
  return errorResponse('Not Found', 404);
}

/**
 * 处理同步相关路由
 */
async function handleSync(request, env, path, method, userId) {
  const pathParts = path.split('/').filter(Boolean);
  
  // 获取同步状态
  if (path === '/api/sync/status' && method === 'GET') {
    const url = new URL(request.url);
    const projectId = url.searchParams.get('projectId');
    
    if (!projectId) {
      return errorResponse('projectId 不能为空');
    }
    
    const projectData = JSON.parse(await env.KV.get(`project:${userId}:${projectId}`) || '{}');
    
    if (!projectData.id) {
      return errorResponse('项目不存在', 404);
    }
    
    // 获取最新版本信息
    const versionData = projectData.version 
      ? JSON.parse(await env.KV.get(`version:${userId}:${projectId}:${projectData.version}`) || '{}')
      : {};
    
    return jsonResponse({
      projectId,
      localVersion: projectData.version || 1,
      lastModified: projectData.modifiedAt,
      lastDevice: versionData.deviceId || 'unknown'
    });
  }
  
  // 推送同步
  if (path === '/api/sync/push' && method === 'POST') {
    const body = await request.json();
    const { projectId, version, deviceId } = body;
    
    if (!projectId || version === undefined) {
      return errorResponse('projectId 和 version 不能为空');
    }
    
    const projectData = JSON.parse(await env.KV.get(`project:${userId}:${projectId}`) || '{}');
    
    if (!projectData.id) {
      return errorResponse('项目不存在', 404);
    }
    
    // 检查版本冲突
    const remoteVersion = projectData.version || 0;
    const hasConflict = remoteVersion > version;
    
    if (hasConflict) {
      // 获取冲突信息
      const remoteVersionData = JSON.parse(
        await env.KV.get(`version:${userId}:${projectId}:${remoteVersion}`) || '{}'
      );
      
      return jsonResponse({
        success: false,
        conflict: true,
        localVersion: version,
        remoteVersion,
        remoteTimestamp: remoteVersionData.timestamp,
        remoteDevice: remoteVersionData.deviceId,
        message: '检测到版本冲突'
      });
    }
    
    // 无冲突，确认推送
    const now = getTimestamp();
    projectData.version = version;
    projectData.modifiedAt = now;
    await env.KV.put(`project:${userId}:${projectId}`, JSON.stringify(projectData));
    
    await env.KV.put(`version:${userId}:${projectId}:${version}`, JSON.stringify({
      timestamp: now,
      deviceId: deviceId || 'unknown'
    }));
    
    return jsonResponse({
      success: true,
      version
    });
  }
  
  // 拉取同步
  if (path === '/api/sync/pull' && method === 'POST') {
    const body = await request.json();
    const { projectId, version } = body;
    
    if (!projectId) {
      return errorResponse('projectId 不能为空');
    }
    
    const projectData = JSON.parse(await env.KV.get(`project:${userId}:${projectId}`) || '{}');
    
    if (!projectData.id) {
      return errorResponse('项目不存在', 404);
    }
    
    const remoteVersion = projectData.version || 0;
    const clientVersion = version || 0;
    
    if (remoteVersion <= clientVersion) {
      // 没有新版本
      return jsonResponse({
        success: true,
        hasUpdate: false,
        version: remoteVersion
      });
    }
    
    // 有新版本，检查是否需要下载文件
    try {
      const file = await env.R2.get(`${userId}/${projectId}.ora`);
      
      return jsonResponse({
        success: true,
        hasUpdate: true,
        version: remoteVersion,
        hasFile: !!file,
        metadata: projectData
      });
    } catch (e) {
      return jsonResponse({
        success: true,
        hasUpdate: true,
        version: remoteVersion,
        hasFile: false,
        metadata: projectData
      });
    }
  }
  
  // 解决冲突
  if (path.match(/^\/api\/sync\/resolve$/) && method === 'POST') {
    const body = await request.json();
    const { projectId, resolution } = body;
    
    if (!projectId || !resolution) {
      return errorResponse('projectId 和 resolution 不能为空');
    }
    
    if (resolution !== 'local' && resolution !== 'remote') {
      return errorResponse('resolution 必须是 local 或 remote');
    }
    
    const projectData = JSON.parse(await env.KV.get(`project:${userId}:${projectId}`) || '{}');
    
    if (!projectData.id) {
      return errorResponse('项目不存在', 404);
    }
    
    if (resolution === 'local') {
      // 客户端会覆盖远程版本，等待客户端上传
      return jsonResponse({
        success: true,
        message: '请上传本地版本'
      });
    } else {
      // 使用远程版本，客户端需要下载
      return jsonResponse({
        success: true,
        message: '请下载远程版本',
        metadata: projectData
      });
    }
  }
  
  return errorResponse('Not Found', 404);
}

// ==================== 主入口 ====================

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    // CORS headers
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Device-Id',
    };

    if (method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      let response;

      // Auth routes (不需要认证)
      if (path === '/api/auth/register' && method === 'POST') {
        response = await handleRegister(request, env);
      } else if (path === '/api/auth/login' && method === 'POST') {
        response = await handleLogin(request, env);
      } else if (path === '/api/auth/phone/send' && method === 'POST') {
        response = await handlePhoneSendCode(request, env);
      } else if (path === '/api/auth/phone/login' && method === 'POST') {
        response = await handlePhoneLogin(request, env);
      } else if (path === '/api/auth/wechat' && method === 'POST') {
        response = await handleWechatLogin(request, env);
      } else if (path === '/api/auth/refresh' && method === 'POST') {
        response = await handleRefreshToken(request, env);
      }
      // Project routes (需要认证)
      else if (path.startsWith('/api/projects')) {
        const userId = await authenticate(request, env);
        if (!userId) {
          response = new Response(JSON.stringify({ error: 'Unauthorized' }), { 
            status: 401, 
            headers: { 'Content-Type': 'application/json' } 
          });
        } else {
          response = await handleProjects(request, env, path, method, userId);
        }
      }
      // Sync routes (需要认证)
      else if (path.startsWith('/api/sync')) {
        const userId = await authenticate(request, env);
        if (!userId) {
          response = new Response(JSON.stringify({ error: 'Unauthorized' }), { 
            status: 401, 
            headers: { 'Content-Type': 'application/json' } 
          });
        } else {
          response = await handleSync(request, env, path, method, userId);
        }
      }
      // Health check
      else if (path === '/health' && method === 'GET') {
        response = jsonResponse({ status: 'ok', timestamp: getTimestamp() });
      }
      else {
        response = new Response('Not Found', { status: 404 });
      }

      // Add CORS headers to response
      const newHeaders = new Headers(response.headers);
      Object.entries(corsHeaders).forEach(([k, v]) => newHeaders.set(k, v));
      return new Response(response.body, {
        status: response.status,
        headers: newHeaders,
      });
    } catch (err) {
      console.error('Unhandled error:', err);
      return new Response(JSON.stringify({ error: err.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json', ...corsHeaders },
      });
    }
  }
};

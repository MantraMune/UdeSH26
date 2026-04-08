#pragma once

#include <imgui.h>
#include <memory>
#include <random>
#include <string>
#include <vector>

#include "ObjLoader.hpp"
#include "application/SubApplication.hpp"
#include "camera.hpp"
#include "rhi/PipelineDesc.hpp"
#include "utils/subAppUtils.hpp"

namespace DrawIndexedSampleData {

struct Vertex {
    Vec3f pos;
    Vec3f normal;
};

// ---- Bounding box ----
struct AABB {
    Vec3f min;
    Vec3f max;
};

struct ViewUBO : public RHI::BaseUBO {
    Mat4f view;
    Mat4f projection;
    Vec4f cameraPosition;
    float near;
    float far;

    GENERATE_UBO_FUNCTIONS(view, projection, cameraPosition, near, far)

    void updateViewUBO(const Camera &camera) {
        Mat4f camera_view = camera.getViewMatrix();
        Mat4f camera_projection = camera.getProjectionMatrix();
        camera_projection[1][1] *= -1;

        view = camera_view;
        projection = camera_projection;
        cameraPosition = Vec4f(camera.getPosition(), 1);
        near = camera.getZNear();
        far = camera.getZFar();
    }
};

}

using namespace DrawIndexedSampleData;

class SubAppDrawIndexed : public SubApplication {
public:
    std::string name() const override { return "DrawIndexed"; }

    void init() override {
        m_camera.init(Vec3f(0.0f, 10.0f, 5.0f), Vec3f(0.0f, 0.0f, -20.0f));

        renderer()->addShaderIncludePaths(m_shaderPath);
        m_pipelineDesc.init({m_shaderPath + "draw_indexed.vert",
                             m_shaderPath + "draw_indexed.frag"});

        // Vertex input layout
        std::vector<RHI::VertexInputBinding> bindings = {
            {0, sizeof(DrawIndexedSampleData::Vertex), RHI::VertexInputRate::eVertex}
        };

        std::vector<RHI::VertexInputAttribute> attributes = {
            {0, 0, RHI::Format::eR32G32B32SF, (uint32_t)offsetof(DrawIndexedSampleData::Vertex, pos)   },
            {1, 0, RHI::Format::eR32G32B32SF, (uint32_t)offsetof(DrawIndexedSampleData::Vertex, normal)}
        };

        // Instance matrix
        bindings.push_back({1, sizeof(GPUBrick), RHI::VertexInputRate::eInstance});

        for (int i = 0; i < 4; i++) {
            attributes.push_back({static_cast<uint32_t>(2 + i),
                                  1,
                                  RHI::Format::eR32G32B32A32SF,
                                  static_cast<uint32_t>(sizeof(float) * 4 * i)});
        }
        attributes.push_back({6,
                              1,
                              RHI::Format::eR32SF,
                              offsetof(GPUBrick, typeID)});

        m_pipelineDesc.setVertexInput(bindings, attributes);
        m_pipelineDesc.setUBO(&m_viewUbo, "viewUbo");

        // Depth Testing
        m_pipelineDesc.depthStencilStateCreateInfo.depthTestEnable = VK_TRUE;
        m_pipelineDesc.depthStencilStateCreateInfo.depthCompareOp = vk::CompareOp::eLess;

        // Culling
        m_pipelineDesc.rasterizationStateCreateInfo.cullMode =
        vk::CullModeFlagBits::eBack;

        renderer()->createPipeline(m_pipelineDesc);;

        // ---- Compute pipeline ----
        m_computePipeline.init({m_shaderPath + "draw_indexed.comp"});
        m_computePipeline.setUBO(&m_viewUbo, "viewUbo");
        renderer()->createPipeline(m_computePipeline);

        // -------------------------- 
        // Load the 5 bricks models 
        // --------------------------
        loadBrickGeometry("models/Brick1.obj", m_brickGeo[0]);
        loadBrickGeometry("models/Brick2.obj", m_brickGeo[1]);
        loadBrickGeometry("models/Brick3.obj", m_brickGeo[2]);
        loadBrickGeometry("models/Brick4.obj", m_brickGeo[3]);
        loadBrickGeometry("models/Brick5.obj", m_brickGeo[4]);

        // ------------------------------- 
        // Build the scene //
        // - castle (4 walls) // 
        // - tower (4 small walls) // 
        // - ruins (several broken walls) 
        // -------------------------------
        buildScene();
    }

    void shutdown() override {
        renderer()->removeShaderIncludePaths(m_shaderPath);
    }

    void handleEvent() override { m_camera.handleEvents(); }
    void resize(int width, int height) override { m_camera.resize(width, height); }
    void animate(float dt) override { m_camera.animate(dt); }

    void render() override {
        m_viewUbo.updateViewUBO(m_camera);

        // Reset indirect buffer before compute 
        for (int i = 0; i < 5; i++) {
            uint32_t zero = 0;
            uint32_t offset = i * sizeof(VkDrawIndexedIndirectCommand) + offsetof(VkDrawIndexedIndirectCommand, instanceCount);
            renderer()->setBuffer(m_indirectBuffer, zero, sizeof(uint32_t), offset);
        }

        RHI::CommandList commandList;

        // ---- Compute pass ----
        commandList.bindPipeline(&m_computePipeline);

        commandList.useResource(&m_computePipeline, 0, 0, &m_brickBuffer); // Input GPU (all bricks)
        commandList.useResource(&m_computePipeline, 0, 1, &m_visibleBuffer); // Output GPU (visible bricks)
        commandList.useResource(&m_computePipeline, 0, 2, &m_indirectBuffer); // Indirect buffer for draw pass
      
        commandList.dispatch((m_totalBricks + 63) / 64, 1, 1);

        commandList.barrier();

        // ---- Render pass ----
        commandList.bindPipeline(&m_pipelineDesc);

        // Bind GLOBAL buffers
        commandList.useVertexBuffer(0, 0, &m_brickGeo[0].vertexBuffer);
        commandList.useIndexBuffer(0, RHI::IndexType::eUint32, &m_brickGeo[0].indexBuffer);
        commandList.useVertexBuffer(1, 0, &m_visibleBuffer);

        // Indirect draw
        commandList.drawIndexedIndirect(
            &m_indirectBuffer,
            0,
            5,
            sizeof(VkDrawIndexedIndirectCommand));
        

        renderer()->compileAndUseCommandList(commandList);
    }

    void displayUI() override {
        ImGui::Begin(name().c_str());

        if (ImGui::Button("Reload Shaders")) {
            m_pipelineDesc.reloadShaders();
        }

        displayInstrumentation("DrawIndexed", m_pipelineDesc, renderer(), gpuTimeHistory);

        ImGui::End();
        m_camera.displayUI();
    }

private:
    struct BrickGeometry { // Ajusted to use global variables (buffers, etc.)
        // Global buffers
        RHI::BufferDesc vertexBuffer;
        RHI::BufferDesc indexBuffer;

        // Local infos (per type of brick)
        uint32_t indexCount = 0;

        // Offsets in the global buffer
        uint32_t firstIndex = 0;
        int32_t vertexOffset = 0;

        // Temporary CPU data (used for fusion with global buffers)
        std::vector<DrawIndexedSampleData::Vertex> cpuVertices;
        std::vector<uint32_t> cpuIndices;
    };

    // Brick types
    BrickGeometry m_brickGeo[5];

    // GPU instance buffers
    RHI::BufferDesc m_instanceBuffers[5];

    // -------------------------------  
    // Wall description  
    // -------------------------------
    struct WallDesc {
        Vec3f origin;
        int width;
        int height;
        float brickW;
        float brickH;
        float yawDeg; // Rotation around Y in degrees
    };

    // ---- GPU-driven brick structure ----
    struct alignas(16) GPUBrick {
        Mat4f model;
        Vec3f min;
        float pad1;
        Vec3f max;
        float pad2;
        float typeID;
        int pad3[3];
    };

    // ---- Indirect Draw Vulkan Structure ----
    struct VkDrawIndexedIndirectCommand {
        uint32_t indexCount;
        uint32_t instanceCount;
        uint32_t firstIndex;
        int32_t vertexOffset;
        uint32_t firstInstance;
    };

    // Instances tables
    std::vector<GPUBrick> m_brickInstances[5]; // Vector containing all the instances
                                               // of the transformation matrix

    // ------------------------------- 
    // Load geometry for one brick type 
    // -------------------------------
    void loadBrickGeometry(const std::string &path, BrickGeometry &geo) {
        TriangleData tri = TriangleMeshLoader::loadTriangleData(path);

        std::vector<DrawIndexedSampleData::Vertex> vertices;
        vertices.reserve(tri.position.size());

        for (size_t i = 0; i < tri.position.size(); i++) {
            vertices.push_back({Vec3f(tri.position[i]),
                                Vec3f(tri.normal[i])});
        }

        // Load data into temporary vectors for fusion
        geo.cpuVertices = vertices;
        geo.cpuIndices = tri.indices;
        geo.indexCount = (uint32_t)tri.indices.size();
    }

    // ------------------------------- 
    // Build a wall 
    // -------------------------------
    void buildWall(const WallDesc &desc, std::default_random_engine &rng) {
        std::uniform_int_distribution<int> pick(0, 4);

        float offsetX = -0.5f * (desc.width - 1) * desc.brickW;
        float offsetY = -0.5f * (desc.height - 1) * desc.brickH;

        Mat4f wall(1.0f);
        wall = glm::translate(wall, desc.origin);
        wall = glm::rotate(wall, glm::radians(desc.yawDeg), Vec3f(0, 1, 0));

        for (int y = 0; y < desc.height; y++) {
            for (int x = 0; x < desc.width; x++) {
                int type = pick(rng);

                GPUBrick m;
                Mat4f local(1.0f);

                local = glm::translate(local,
                                       Vec3f(offsetX + x * desc.brickW,
                                             offsetY + y * desc.brickH,
                                             0));

                m.model = wall * local;

                // Compute bounds in local space
                auto &geo = m_brickGeo[type];
                if (!geo.cpuVertices.empty()) {
                    Vec3f min = geo.cpuVertices[0].pos;
                    Vec3f max = geo.cpuVertices[0].pos;
                    for (auto &v : geo.cpuVertices) {
                        min = glm::min(min, v.pos);
                        max = glm::max(max, v.pos);
                    }
                    m.min = min;
                    m.max = max;
                } else {
                    m.min = Vec3f(0.0f);
                    m.max = Vec3f(0.0f);
                }

                m_brickInstances[type].push_back(m);
            }
        }
    }
    // ------------------------------- 
    // Build ruins: shorter, broken walls 
    // ------------------------------- 
    void buildRuins(std::default_random_engine& rng) 
    {
        const float brickW = 1.1f; 
        const float brickH = 0.6f; 
        // Base ruins center 
        float cx = -25.0f; 
        float cz = -26.0f; 
        
        float zOffset = 6.5f; // For better clarity
        
        // RUIN WALL 1 — center wall 
        WallDesc center
        { 
            Vec3f(cx + 1.0f, 0.0f, cz), 
            3, 1, 
            brickW, brickH, 
            10.0f 
        }; 
        // RUIN WALL 2 — front wall 
        WallDesc front
        { 
            Vec3f(cx, 0.0f, cz + zOffset), 
            4, 2, 
            brickW, brickH, 
            -20.0f 
        }; 
        // RUIN WALL 3 — back wall 
        WallDesc back
        { 
            Vec3f(cx, 0.0f, cz - zOffset), 
            5, 2, 
            brickW, brickH, 
            35.0f 
        }; 
        
        buildWall(back, rng); 
        buildWall(center, rng); 
        buildWall(front, rng); } 
    
    // ------------------------------- 
    // Build castle: 4 walls forming a rectangle 
    // ------------------------------- 
    void buildCastle(std::default_random_engine& rng) 
    { 
        const float brickW = 1.1f; 
        const float brickH = 0.6f;

        // Rectangle centered around z ~ -30 
        WallDesc front 
        { 
            Vec3f(0.0f, 0.0f, -17.0f), 
            22, 6, 
            brickW, brickH, 
            0.0f 
        }; 
        WallDesc back 
        { 
            Vec3f(0.0f, 0.0f, -35.0f), 
            22, 6, 
            brickW, brickH, 
            0.0f 
        }; 
        WallDesc left 
        { 
            Vec3f(-7.6f, 0.0f, -26.0f), 
            10, 6, 
            brickW, brickH, 
            90.0f 
        }; 
        WallDesc right 
        { 
            Vec3f(11.0f, 0.0f, -26.0f), 
            10, 6, 
            brickW, brickH, 
            90.0f 
        }; 
        
        buildWall(front, rng); 
        buildWall(back, rng); 
        buildWall(left, rng); 
        buildWall(right, rng); 
    } 
    // ------------------------------- 
    // Build tower: small square of walls 
    // ------------------------------- 
    void buildTower(std::default_random_engine& rng) 
    { 
        const float brickW = 1.1f; 
        const float brickH = 0.6f; 
        
        // Tower center 
        float cx = 25.0f; 
        float cz = -26.0f; 
        
        // Tower dimensions 
        int width = 5; 
        int height = 8; // Half-size for spacing 
        float half = (width * brickW) * 0.5f; 
        float gap = 2.5f; // Avoid overlapping 
        
        // FRONT wall (facing camera) 
        WallDesc front
        { 
            Vec3f(cx + 1.0f, 0.0f, cz - half - gap), 
            width, height, 
            brickW, brickH, 
            0.0f
        }; 
        // BACK wall 
        WallDesc back
        { 
            Vec3f(cx + 1.0f, 0.0f, cz + half + gap), 
            width, height, 
            brickW, brickH, 
            180.0f
        }; 
        // LEFT wall 
        WallDesc left
        { 
            Vec3f(cx - half - gap + 1.0f, 0.0f, cz), 
            width, height, 
            brickW, brickH, 
            90.0f
        }; 
        // RIGHT wall 
        WallDesc right
        { 
            Vec3f(cx + half + gap + 1.0f, 0.0f, cz), 
            width, height, 
            brickW, brickH, 
            270.0f
        }; 
        
        buildWall(front, rng); 
        buildWall(back, rng); 
        buildWall(left, rng); 
        buildWall(right, rng); 
    } 
    // ------------------------------- 
    // Build the scene 
    // -------------------------------
    void buildScene() {

        const uint MAX_INSTANCES_PER_TYPE = 10000;

        for (int i = 0; i < 5; i++) {
            m_brickInstances[i].clear();
        }

        std::default_random_engine rng;

        buildCastle(rng);
        buildTower(rng);
        buildRuins(rng);

        // ---------------------------
        // Build GLOBAL buffers (vertex and index)
        // ---------------------------
        std::vector<DrawIndexedSampleData::Vertex> globalVertices;
        std::vector<uint32_t> globalIndices;

        uint32_t vertexOffset = 0;
        uint32_t indexOffset = 0;

        for (int i = 0; i < 5; i++) {
            auto &geo = m_brickGeo[i];

            // Step 1 - Save offsets
            geo.vertexOffset = vertexOffset;
            geo.firstIndex = indexOffset;

            // Step 2 - Copy vertices
            globalVertices.insert(globalVertices.end(),
                                  geo.cpuVertices.begin(),
                                  geo.cpuVertices.end());

            // Step 3 - Copy indices with offset
            for (uint32_t idx : geo.cpuIndices) {
                globalIndices.push_back(idx);
            }

            vertexOffset += (uint32_t)geo.cpuVertices.size();
            indexOffset += (uint32_t)geo.cpuIndices.size();

        }

        // ---------------------------
        // Upload GLOBAL buffers
        // ---------------------------

        // Step 1 - Vertex buffer
        m_brickGeo[0].vertexBuffer.specialUsage = RHI::Usage::eVertex;
        createAndUploadBuffer(*renderer(), m_brickGeo[0].vertexBuffer,
                              globalVertices, "GlobalVB");

        // Step 2 - Index buffer
        m_brickGeo[0].indexBuffer.specialUsage = RHI::Usage::eIndex;
        createAndUploadBuffer(*renderer(), m_brickGeo[0].indexBuffer,
                              globalIndices, "GlobalIB");

        // ---- Creating GPU buffers ----
        std::vector<GPUBrick> gpuBricks;

        for (int i = 0; i < 5; i++) {
            for (auto& inst : m_brickInstances[i]) {
                GPUBrick b;
                b.model = inst.model;
                b.min = inst.min;
                b.max = inst.max;
                b.typeID = i;
                gpuBricks.push_back(b);
            }
        }

        m_totalBricks = (uint32_t)gpuBricks.size();

        m_brickBuffer.shaderAccess = RHI::ShaderAccessMode::eReadOnly;
        createAndUploadBuffer(*renderer(), m_brickBuffer, gpuBricks, "BrickBuffer");

        uint32_t maxCount = (uint32_t)gpuBricks.size();

        m_visibleBuffer.size = sizeof(GPUBrick) * MAX_INSTANCES_PER_TYPE * 5;
        m_visibleBuffer.shaderAccess = RHI::ShaderAccessMode::eReadWrite;
        m_visibleBuffer.specialUsage = RHI::Usage::eVertex;

        renderer()->createBuffer(m_visibleBuffer);

        // ---- Setup Indirect Draw ----
        std::vector<VkDrawIndexedIndirectCommand> cmds(5);
        for (int i = 0; i < 5; i++) {
            cmds[i].indexCount = m_brickGeo[i].indexCount;
            cmds[i].instanceCount = 0;
            cmds[i].firstIndex = m_brickGeo[i].firstIndex;
            cmds[i].vertexOffset = m_brickGeo[i].vertexOffset;
            cmds[i].firstInstance = i * MAX_INSTANCES_PER_TYPE;
        }

        createAndUploadBuffer(*renderer(), m_indirectBuffer, cmds, "IndirectBuffer");

    }

private:
    const std::string m_shaderPath = "src/drawSamples/DrawIndexed/shaders/";
    Camera m_camera;
    DrawIndexedSampleData::ViewUBO m_viewUbo; // Uniform View Buffer

    // ---- Pipelines ----
    RHI::PipelineDesc m_pipelineDesc; // Graphics (vertex and fragment)
    RHI::PipelineDesc m_computePipeline; // Complex mathematics (compute)

    std::deque<nanosecondsF> gpuTimeHistory;
    RHI::BufferDesc m_indirectBuffer; // Indirect Drawing buffer

    // ---- SSBO ----
    RHI::BufferDesc m_brickBuffer; // Input (for all bricks)
    RHI::BufferDesc m_visibleBuffer; // Output (visibles models)
    RHI::BufferDesc m_counterBuffer; // Counter (for triangles and instances)

    uint32_t m_totalBricks = 0;


};

REGISTER_SUBAPP("DrawIndexed", SubAppDrawIndexed)


package com.farmrpg.jobs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class JobSelectionScreen extends Screen {

    // 판타지 모험가 길드 배경 이미지
    private static final ResourceLocation JOB_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(
                    FarmRPGJobs.MODID,
                    "textures/gui/job_background.png"
            );

    private enum Page {
        MAIN, COMBAT, LIFE, DETAIL
    }

    private Page currentPage = Page.MAIN;
    private Page previousPage = Page.MAIN;

    private String selectedJob = "";
    private String selectedDescription = "";
    private String selectedFeatures = "";

    private static final String[] COMBAT_JOBS = {
            "전사", "궁수", "마법사", "성기사",
            "레인저", "암살자", "성직자"
    };

    private static final String[] LIFE_JOBS = {
            "약초꾼", "세공사", "광부"
    };

    public JobSelectionScreen() {
        super(Component.literal("Farm RPG - 직업 선택"));
    }

    @Override
    protected void init() {
        super.init();
        showCurrentPage();
    }

    private void showCurrentPage() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (currentPage == Page.MAIN) {

            addButton("전투 직업",
                    centerX - 105, centerY,
                    100, 20,
                    () -> openPage(Page.COMBAT));

            addButton("생활 직업",
                    centerX + 5, centerY,
                    100, 20,
                    () -> openPage(Page.LIFE));

        } else if (currentPage == Page.COMBAT
                || currentPage == Page.LIFE) {

            String[] jobs = currentPage == Page.COMBAT
                    ? COMBAT_JOBS : LIFE_JOBS;

            int startX = centerX - 105;
            int startY = centerY - 55;

            for (int i = 0; i < jobs.length; i++) {

                final String jobName = jobs[i];

                int x = startX + (i % 2) * 110;
                int y = startY + (i / 2) * 25;

                addButton(jobName,
                        x, y, 100, 20,
                        () -> openJobDetail(jobName));
            }

            addButton("뒤로 가기",
                    centerX - 50, centerY + 55,
                    100, 20,
                    () -> openPage(Page.MAIN));

        } else if (currentPage == Page.DETAIL) {

            addButton("뒤로 가기",
                    centerX - 105, centerY + 95,
                    100, 20,
                    () -> openPage(previousPage));

            addButton("직업 선택",
                    centerX + 5, centerY + 95,
                    100, 20,
                    this::confirmJob);
        }
    }

    private void addButton(
            String text,
            int x, int y,
            int width, int height,
            Runnable action
    ) {
        this.addRenderableWidget(
                Button.builder(
                        Component.literal(text),
                        button -> action.run()
                ).bounds(x, y, width, height).build()
        );
    }

    private void openPage(Page page) {
        currentPage = page;
        showCurrentPage();
    }

    private void openJobDetail(String jobName) {

        previousPage = currentPage;
        currentPage = Page.DETAIL;

        selectedJob = jobName;

        switch (jobName) {
            case "전사" -> {
                selectedDescription = "검과 방패를 사용하는 근접 전투 직업";
                selectedFeatures = "근접 공격 / 방어 / 생존력";
            }
            case "궁수" -> {
                selectedDescription = "활을 이용한 빠른 원거리 공격";
                selectedFeatures = "연속 사격 / 원거리 공격";
            }
            case "마법사" -> {
                selectedDescription = "마법으로 적을 공격하는 직업";
                selectedFeatures = "마법 공격 / 범위 공격";
            }
            case "성기사" -> {
                selectedDescription = "방어와 보호에 특화된 직업";
                selectedFeatures = "방어 / 보호 / 지원";
            }
            case "레인저" -> {
                selectedDescription = "정밀한 원거리 공격에 특화된 직업";
                selectedFeatures = "정밀 사격 / 단일 대상 공격";
            }
            case "암살자" -> {
                selectedDescription = "빠른 기습 공격에 특화된 직업";
                selectedFeatures = "기습 / 치명타 / 기동성";
            }
            case "성직자" -> {
                selectedDescription = "아군의 회복과 지원을 담당하는 직업";
                selectedFeatures = "회복 / 강화 / 지원";
            }
            case "약초꾼" -> {
                selectedDescription = "희귀한 약초를 채집하는 생활 직업";
                selectedFeatures = "약초 채집 / 희귀 재료";
            }
            case "세공사" -> {
                selectedDescription = "광물로 장신구를 제작하는 생활 직업";
                selectedFeatures = "반지 / 목걸이 / 장신구 제작";
            }
            case "광부" -> {
                selectedDescription = "광물을 효율적으로 채굴하는 생활 직업";
                selectedFeatures = "채굴 효율 / 광물 수집";
            }
            default -> {
                selectedDescription = "";
                selectedFeatures = "";
            }
        }

        showCurrentPage();
    }

    private void confirmJob() {
        // 아직 서버에 직업을 저장하지 않습니다.
        // 다음 단계에서 실제 직업 선택 기능을 연결합니다.
    }
    @Override
    public void renderBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // 새로운 판타지 모험가 길드 배경
        graphics.pose().pushPose();
        graphics.pose().scale(this.width / 1672f, this.height / 941f, 1f);
        graphics.blit(
                JOB_BACKGROUND,
                0, 0,
                0, 0,
                1672, 941,
                1672, 941
        );
        graphics.pose().popPose();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;


        if (currentPage == Page.DETAIL) {

            graphics.drawCenteredString(
                    this.font,
                    selectedJob + " - 직업 상세 정보",
                    centerX,
                    centerY - 85,
                    0xFFFFFF
            );

            // 직업 이미지 표시 위치
            int imageX = centerX - 110;
            int imageY = centerY - 75;
            int imageWidth = 90;
            int imageHeight = 150;

            // 선택한 직업에 맞는 이미지 파일 지정
            String jobTexture = switch (selectedJob) {
                case "전사" -> "warrior";
                case "궁수" -> "archer";
                case "마법사" -> "mage";
                case "성기사" -> "paladin";
                case "레인저" -> "ranger";
                case "암살자" -> "assassin";
                case "성직자" -> "cleric";
                case "약초꾼" -> "herbalist";
                case "세공사" -> "jeweler";
                case "광부" -> "miner";
                default -> "warrior";
            };
            // 선택한 직업의 PNG 이미지 경로
            ResourceLocation jobImage = ResourceLocation.fromNamespaceAndPath(
                    FarmRPGJobs.MODID,
                    "textures/gui/jobs/" + jobTexture + ".png"
            );

// 원본 307x512 직업 카드를 90x150으로 축소
            graphics.pose().pushPose();

            graphics.pose().translate(imageX, imageY, 0);

            int textureWidth = selectedJob.equals("마법사") || selectedJob.equals("약초꾼") ? 308 : 307;
            graphics.pose().scale(
                    90f / textureWidth,
                    150f / 512f,
                    1f
            );

            graphics.blit(
                    jobImage,
                    0, 0,
                    0, 0,
                    textureWidth, 512,
                    textureWidth, 512
            );

            graphics.pose().popPose();




            // Separate the text from the busy background while retaining the card layout.
            int panelLeft = centerX - 15;
            int panelTop = centerY - 75;
            int panelRight = Math.min(centerX + 145, this.width - 5);
            int panelBottom = centerY + 75;
            graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xCCB28A45);
            graphics.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xD91A1410);

            int textX = panelLeft + 8;
            int textWidth = Math.max(1, panelRight - panelLeft - 16);
            graphics.drawString(this.font, selectedJob, textX, panelTop + 12, 0xFFFF55);

            int textY = panelTop + 35;
            for (var line : this.font.split(Component.literal(selectedDescription), textWidth)) {
                if (textY + this.font.lineHeight > panelBottom - 5) break;
                graphics.drawString(this.font, line, textX, textY, 0xFFFFFF);
                textY += this.font.lineHeight + 2;
            }
            textY += 12;
            for (var line : this.font.split(Component.literal(selectedFeatures), textWidth)) {
                if (textY + this.font.lineHeight > panelBottom - 5) break;
                graphics.drawString(this.font, line, textX, textY, 0xD6C9AE);
                textY += this.font.lineHeight + 2;
            }

        } else {

            String title = switch (currentPage) {
                case COMBAT -> "전투 직업 선택";
                case LIFE -> "생활 직업 선택";
                default -> "Farm RPG - 직업 선택";
            };

            graphics.drawCenteredString(
                    this.font,
                    title,
                    centerX,
                    centerY - 85,
                    0xFFFFFF
            );
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
